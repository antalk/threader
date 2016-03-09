package com.paragonict.webapp.threader.services.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.FetchProfile;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.UIDFolder;
import javax.mail.Flags.Flag;

import org.apache.tapestry5.grid.SortConstraint;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.util.TapestryException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.paragonict.webapp.threader.Constants;
import com.paragonict.webapp.threader.entities.Account.PROTOCOL;
import com.paragonict.webapp.threader.entities.Folder;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailService;
import com.paragonict.webapp.threader.services.IMailStore;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.smtp.SMTPMessage;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/*
 * 
 * 
 * 
 */
public class MailServiceImpl implements IMailService {
	
	private Properties systemProperties  = System.getProperties();
	
	@Inject
	private HibernateSessionManager hsm;
	
	@Inject
	private IAccountService as;
	
	// per thread object,closes open folders after request
	@Inject
	private IMailStore store;

	@Inject
	@Symbol(value=Constants.SYMBOL_MAIL_DEBUG)
	private boolean debug;
	
	private Session _mailSession;
	
	private CacheManager _manager;
	
	
	@PostInjection
	public void init() {
		// creates a session usable for all clients
		_mailSession =  Session.getInstance(systemProperties,new Authenticator() {
        	@Override
        	protected PasswordAuthentication getPasswordAuthentication() {
        		return new PasswordAuthentication(as.getAccount().getAccountName(), as.getAccount().getPassword());
        	}
         });
		if (debug) _mailSession.setDebug(true);
		_manager = CacheManager.newInstance(this.getClass().getResourceAsStream("/mailcache.xml"));
		
	}
	
	public Folder getFolders(boolean renew) throws MessagingException {
		Folder rootFolder;
		
		if (as.isLoggedIn()) {
			List<Folder> folderList = hsm.getSession().createCriteria(Folder.class).add(Restrictions.eq("account", as.getAccount())).list();
			if (folderList.isEmpty() || renew ) {
				
				List<Folder> oldFolders= hsm.getSession().createCriteria(Folder.class).add(Restrictions.eq("account", as.getAccount())).list();
				for (Folder o :oldFolders){
					hsm.getSession().delete(o);
				}
				hsm.commit();
				
				//javax.mail.Folder[] folders = mailSession.getStore().getDefaultFolder().list("%");
				
				javax.mail.Folder root = store.getStore(_mailSession).getDefaultFolder();
				System.err.println("parent :"+ root);
				
				rootFolder = new Folder();
				rootFolder.setAccount(as.getAccount());
				rootFolder.setName("root");
				hsm.getSession().persist(rootFolder);
				
				persistFolders(rootFolder,Arrays.asList(root.list("%")));
				hsm.commit();
				
				// now then, POP3 does not support folders, lets create them ourselves...
				if (root instanceof com.sun.mail.pop3.DefaultFolder) {
					// Draft
					Folder draftFolder = new Folder();
					draftFolder.setAccount(as.getAccount());
					draftFolder.setName("Drafts");
					draftFolder.setParent(rootFolder);
					draftFolder.setLabel("DRAFTS");
					hsm.getSession().persist(draftFolder);
				}
				hsm.commit();
				
			} else {
				rootFolder = (Folder) hsm.getSession().createCriteria(Folder.class).add(Restrictions.eq("name", "root")).add(Restrictions.eq("account", as.getAccount())).uniqueResult();
			}
			return rootFolder;
		} else {
			throw new MessagingException("User not logged in!");
		}
	}
	
	@Override
	public LocalMessage getLocalMessage(String UID) throws MessagingException {
		return (LocalMessage) hsm.getSession().load(LocalMessage.class, UID);
	}
	
	
	
	private void persistFolders(Folder parent, List<javax.mail.Folder> list) throws MessagingException {
		for (javax.mail.Folder f: list) {
			// persist f with parent

			Folder childFolder = new Folder();
			childFolder.setAccount(as.getAccount());
			childFolder.setName(f.getFullName());
			childFolder.setLabel(f.getName());
			childFolder.setParent(parent);
			childFolder.setHasChilds(false);
			
			if ((f.getType() & javax.mail.Folder.HOLDS_MESSAGES) ==1) {
				childFolder.setUnreadMsgs(f.getUnreadMessageCount());	
			}
			
			hsm.getSession().persist(childFolder);
			
			if ((f.getType() & javax.mail.Folder.HOLDS_FOLDERS) ==2) {
				final List<javax.mail.Folder> childs = Arrays.asList(f.list("%"));
				if (!childs.isEmpty()) {
					childFolder.setHasChilds(true);
					hsm.getSession().persist(childFolder);
					persistFolders(childFolder, childs);
				}
			}
		}
	}
	
	
	public List<LocalMessage> getMessages(final String folder,final int start,final int end, final int mailTotal, final SortConstraint sc) throws MessagingException {
		//final List<Message> clientMsgs = new LinkedList<Message>();
		// simple version..
		if (folder != null) {
			
			// first check if mailTotal is still exact as nr of db rows..
			// onyl if there are MORE msgs on server, add them first to the db.
			// deletes will be immediately
			// and remote deletes will show up when you try to open the message.
			Criteria criteria = hsm.getSession().createCriteria(LocalMessage.class);
			//applyAdditionalConstraints(criteria);
	        criteria.setProjection(Projections.rowCount());
	        Number nrofDBRows = (Number)criteria.uniqueResult();
	        
	        
	        
	        if (mailTotal > nrofDBRows.intValue()) {
	        	// this is also wrong the results coudl be the same but different message could be added / deleted from a 
	        	// remote client..... (with the same account.... though..)
	        	// maybe a possible solution is to periodically check for changes and update the local db as such...
	        	// or add some listeners to mailserver updates...
	        	
	        	// for now assume additions only..
	        	int nrToRetrieve = mailTotal-nrofDBRows.intValue();
	        	
	        	System.err.println("GET MSGS" + System.currentTimeMillis());
				javax.mail.Folder f = store.getStore(_mailSession).getFolder(folder);
				if (!f.isOpen()) {
					f.open(javax.mail.Folder.READ_ONLY); // read only, RW will be done per individual message
				}
				
				Message[] msgs = new Message[nrToRetrieve];
				msgs = f.getMessages(nrofDBRows.intValue()+1,mailTotal); // fetch the difference...
				final FetchProfile fp = new FetchProfile();
				fp.add(javax.mail.FetchProfile.Item.ENVELOPE);
				f.fetch(msgs, fp);
				
				// store by uid in DB
				for (Message m: msgs) {
					hsm.getSession().save(new LocalMessage(m, as.getAccount()));
				}
				hsm.commit(); // done
	        }

			// now query, TODO: ordering and filtering
			criteria = hsm.getSession().createCriteria(LocalMessage.class);
			criteria.add(Restrictions.eq("folder", folder.toUpperCase()));
			criteria.setFirstResult(start).setMaxResults((end+1)-start);
			return criteria.list();
		}
		return Collections.emptyList();
	}
	
	
		
	
	@Override
	public Integer getNrOfMessages(String folder) throws MessagingException {
		
		if (as.getAccount().getProtocol().equals(PROTOCOL.pop3) ||
				as.getAccount().getProtocol().equals(PROTOCOL.pops)) {
			if (!folder.equalsIgnoreCase("INBOX")) {
				// get nr of message locally stored only
				return hsm.getSession().createCriteria(LocalMessage.class).add(Restrictions.eq("folder", folder.toUpperCase())).list().size();
			}
		}
		// IMAP(S) or INBOX
		if (folder != null) {
			javax.mail.Folder f  = store.getStore(_mailSession).getFolder(folder);
			f.open(javax.mail.Folder.READ_ONLY);
			return f.getMessageCount();
		}
		return 0;
	}
	
	

	@Override
	public SMTPMessage createMessage() {
		return new SMTPMessage(_mailSession);
	}
	
	@Override
	public Message getMailMessage(final LocalMessage localMsg) throws MessagingException {
		final String UID = localMsg.getUID();
		final String folderName = localMsg.getFolder();
		
		final Cache myCache = getCache(localMsg);
		
		if (myCache.isKeyInCache(UID)) {
			final Element e =  myCache.get(UID);
			if (e!=null) {
				final Message msg = (Message) e.getObjectValue();
				final javax.mail.Folder f = msg.getFolder();
				if (!f.isOpen()) {
					store.registerFolder(f);
					f.open(javax.mail.Folder.READ_WRITE);
				}
				return msg;
			}
		}
		
		// the folder NEEDS to be open when accessing the retrieved message.. even if it lives in cache!
		javax.mail.Folder f = store.getStore(_mailSession).getFolder(folderName);
		if (!f.isOpen()) {
			f.open(javax.mail.Folder.READ_WRITE); // read write otherwise we cant set flags the referenced message object..
		}
		
		Message message = null;
		
		// now switch between imap and pop3.
		if (f instanceof IMAPFolder) {
			// so now what if this msg is gone ???
			message = (((IMAPFolder)f).getMessageByUID(Long.valueOf(UID)));
		} else {
			final POP3Folder pop3folder = (POP3Folder) f;
			Message[] msgs = f.getMessages();
			
			final FetchProfile fp = new FetchProfile();
			fp.add(UIDFolder.FetchProfileItem.UID);
			pop3folder.fetch(msgs, fp);
			for (Message m: msgs) {
				if (pop3folder.getUID(m).equals(UID)) {
					message = m;
					break;
				}
			}
		}
		if (message == null) {
			throw new MessagingException("Message with UID ["+UID+"] was not on the server anymore !");
			
			// TODO:/ delete this LocalMessage from DB?
		}
		myCache.put(new Element(localMsg.getUID(), message));
		return message;
	}

	@Override
	public boolean isMessageRead(LocalMessage message) throws MessagingException {
		if (isRemoteFolder(message.getFolder())) {
			return getMailMessage(message).getFlags().contains(Flag.SEEN);	
		}
		return false; // cant determine.
		
	}

	
	/**
     * Return the primary text content of the message.
     * Copied from the interwebs
     */
	public String getMessageContent(Part p) throws IOException,MessagingException {
	    if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            //textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = getMessageContent(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getMessageContent(bp);
                    if (s != null)
                        return s;
                } else {
                    return getMessageContent(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getMessageContent(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }
        return null;
    }

	//----PRIVATE METHODS -----///
	
	// use this method to get a cache for the current user based on email address
	private Cache getCache(final LocalMessage localMessage) {
		// security check
		if (!(localMessage.getAccount().compareTo(as.getAccount().getId()) == 0)) {
			//  It is NOT your message !
			throw new TapestryException("[Security] Requested message does not belong to current user !", this, new IllegalAccessException());
		}
		
		final String cacheKey = as.getAccount().getEmailAddress();
		if (!_manager.cacheExists(cacheKey)) {
			_manager.addCache(cacheKey);
		}
		return _manager.getCache(cacheKey);
		
	}
		
	private boolean isRemoteFolder(final String folder) {
		if (as.getAccount().getProtocol().equals(PROTOCOL.pop3) ||
				as.getAccount().getProtocol().equals(PROTOCOL.pops)) {
			if (!folder.equalsIgnoreCase("INBOX")) {
				return false;
			}
		}
		return true;
	}
}
