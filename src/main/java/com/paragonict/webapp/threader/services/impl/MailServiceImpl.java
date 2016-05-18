package com.paragonict.webapp.threader.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;

import org.apache.commons.mail.util.MimeMessageParser;
import org.apache.tapestry5.grid.SortConstraint;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.LoggerSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.util.TapestryException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;

import com.paragonict.webapp.threader.Constants;
import com.paragonict.webapp.threader.entities.Account.PROTOCOL;
import com.paragonict.webapp.threader.entities.Folder;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IApplicationError;
import com.paragonict.webapp.threader.services.IMailService;
import com.paragonict.webapp.threader.services.IMailSession;
import com.paragonict.webapp.threader.services.IMailStore;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPFolder.FetchProfileItem;
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
	
	@Inject
	private HibernateSessionManager hsm;
	
	@Inject
	private IAccountService as;
	
	@Inject
	private IMailSession session;
	
	// per thread object,closes open folders after request
	@Inject
	private IMailStore store;
	
	@Inject
	private LoggerSource logSource;
	
	@Inject
	private IApplicationError appErrors;

	@Inject
	@Symbol(value=Constants.SYMBOL_MAIL_DEBUG)
	private boolean debug;
	
	
	private CacheManager _manager;
	
	private Logger logger;
	
	
	@PostInjection
	public void init() {
		_manager = CacheManager.newInstance(this.getClass().getResourceAsStream("/mailcache.xml"));
		logger = logSource.getLogger(this.getClass());
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
				
				javax.mail.Folder root = store.getDefaultFolder();
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
					
					//TODO: sent and deleted ?? 
					
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
			
			List<javax.mail.Folder> childs = new ArrayList<javax.mail.Folder>();
			if ((f.getType() & javax.mail.Folder.HOLDS_FOLDERS) ==2) {
				childs = Arrays.asList(f.list("%"));
			}
			childFolder.setHasChilds(!childs.isEmpty());
			
			if (f instanceof IMAPFolder) {
				// set some more stuff
				childFolder.setUidValidity(((IMAPFolder)f).getUIDValidity());
			}
			hsm.getSession().persist(childFolder);
			
			if (!childs.isEmpty()) {
				persistFolders(childFolder, childs);
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
			
			// FETCH ALL LOCAL UIDS FROM the selected folder
			final List<String> storedUIDs = hsm.getSession().getNamedQuery(LocalMessage.GET_ALL_UIDS).
				setLong("accountid", as.getAccount().getId()).
				setString("folder",folder).list();
			
			int nrofDBRows = storedUIDs.size();
			
	        logger.debug("Nr. of local stored messages {}, number of message on server {}", nrofDBRows,mailTotal);
	        
	        if (mailTotal > nrofDBRows) {
	        	// this is also wrong the results coudl be the same but different message could be added / deleted from a 
	        	// remote client..... (with the same account.... though..)
	        	// maybe a possible solution is to periodically check for changes and update the local db as such...
	        	// or add some listeners to mailserver updates...
	        	
	        	javax.mail.Folder f = store.getFolder(folder);
				if (!f.isOpen()) {
					f.open(javax.mail.Folder.READ_ONLY); // read only, RW will be done per individual message
				}
				
				final Message[] msgs = f.getMessages();
				final FetchProfile fp = new FetchProfile();
				fp.add(javax.mail.UIDFolder.FetchProfileItem.UID);
				f.fetch(msgs, fp);
				
				LocalMessage possiblyNewMsg = null;
				for (Message m:msgs) {
					// filter the message we already have
					possiblyNewMsg = new LocalMessage(m, as.getAccount());
					
					if (storedUIDs.contains(possiblyNewMsg.getUID())) {
						logger.debug("Message with UID {} already stored, skipping",possiblyNewMsg.getUID());
						storedUIDs.remove(possiblyNewMsg.getUID()); // remove from list
					} else {
						// ah new mssg
						logger.debug("Adding new localmessage from {} ",m);
						hsm.getSession().save(possiblyNewMsg);
						nrofDBRows++;
					}
					// then a quick break if we are in sync
					if (nrofDBRows == mailTotal) {
						logger.debug("Number of messages {} on server and in DB match, stop searching",nrofDBRows);
						break;// dont bother in searching the rest..
					}
				}
				// now we have stored ALL new msgs
				hsm.commit(); // done
	        }

			// now query, TODO: ordering and filtering /search can only be done on the COMPLETE set of rows.. not the subset returning
			Criteria criteria = hsm.getSession().createCriteria(LocalMessage.class);
			criteria.add(Restrictions.eq("folder", folder.toUpperCase()));
			criteria.add(Restrictions.eq("account", as.getAccount().getId()));
			criteria.addOrder(Order.desc("sentDate"));
			
			//criteria.setFirstResult(start).setMaxResults((end+1)-start);
			
			
			return criteria.list().subList(start, end+1);
		}
		return Collections.emptyList();
	}
	
	
		
	
	@Override
	public Integer getNrOfMessages(String folder) throws MessagingException {
		
		if (as.getAccount().getProtocol().equals(PROTOCOL.pop3) ||
				as.getAccount().getProtocol().equals(PROTOCOL.pops)) {
			if (!folder.equalsIgnoreCase("INBOX")) {
				// get nr of message locally stored only
				return ((Long) hsm.getSession().createCriteria(LocalMessage.class)
						.add(Restrictions.eq("folder", folder.toUpperCase()))
						.add(Restrictions.eq("account", as.getAccount().getId()))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			}
		}
		// IMAP(S) or INBOX
		if (folder != null) {
			javax.mail.Folder f  = store.getFolder(folder);
			f.open(javax.mail.Folder.READ_ONLY);
			return f.getMessageCount();
		}
		return 0;
	}
	
	

	@Override
	public SMTPMessage createMessage() {
		return new SMTPMessage(session.getSession());
	}
	
	@Override
	public Message getMailMessage(final LocalMessage localMsg) throws MessagingException {
		final String UID = localMsg.getUID();
		final String folderName = localMsg.getFolder();
		
		final Cache myCache = getCache(localMsg);
		
		logger.trace("Checking cache for UID {}", UID);
		if (myCache.isKeyInCache(UID)) {
			logger.trace("Key is in cache trying to fetch item quietly for UID {} ",UID);
			final Element e =  myCache.getQuiet(UID);
			if (e!=null) {
				logger.trace("Element {} is in cache, fetch objectValue",e);
					
				final Message msg = (Message) e.getObjectValue();
				final javax.mail.Folder f = msg.getFolder();
				if (!f.isOpen()) {
					logger.debug("folder {} is not open",f);
					store.registerFolder(f);
					f.open(javax.mail.Folder.READ_WRITE);
				}
				return msg;
			}
		}
		
		// the folder NEEDS to be open when accessing the retrieved message.. even if it lives in cache!
		javax.mail.Folder f = store.getFolder(folderName);
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
			appErrors.addApplicationError("Message was not on the server anymore");
			// TODO:/ delete this LocalMessage from DB?
			
			hsm.getSession().delete(localMsg);
			hsm.commit();
			
		} else {
			// do not put null items in the cache
			myCache.put(new Element(localMsg.getUID(), message));
		}
		return message;
	}

	@Override
	public boolean isMessageRead(LocalMessage message) throws MessagingException {
		if (isRemoteFolder(message.getFolder())) {
			final Message orgMsg = getMailMessage(message);
			if (orgMsg!=null) {
				return orgMsg.getFlags().contains(Flag.SEEN);	
			}
			appErrors.addApplicationError("Message is not on server anymore");
		}
		return false; // cant determine.
		
	}

	/* TODO: make this transactional and more robust..*/
	@Override
	public boolean deleteMailMessage(final String... UIDs) throws MessagingException {
		boolean result = true;
		if (UIDs.length > 0) {
			
			
			LocalMessage lm = null;
			final List<Message> remoteMsgsToDelete = new ArrayList<Message>();
			
			for (String UID : UIDs) {
				try {
					lm = getLocalMessage(UID);
					if (isRemoteFolder(lm.getFolder())) {
						remoteMsgsToDelete.add(getMailMessage(lm));
					}
					// remove from db also
					hsm.getSession().delete(lm);
				} catch (Exception e) {
					// TODO:
					result = false;
				}
			}
			
			if (!remoteMsgsToDelete.isEmpty()) {
				
				final Flags deleted = new Flags(Flag.DELETED);
				try {
					// the first message contains the affected folder.
					final javax.mail.Folder affectedFolder = remoteMsgsToDelete.get(0).getFolder();
					
					affectedFolder.setFlags(remoteMsgsToDelete.toArray(new Message[]{}), deleted,true);
					affectedFolder. close(true);
				 
					hsm.commit();
				} catch (MessagingException e) {
					// TODO try saving my messages???
					e.printStackTrace();
				}
			}
		}		
		return result;
	}
	
	/**
     * Return the primary text content of the message.
     * Copied from the interwebs
     */
	public String getMessageContent(final LocalMessage m) {
		try {
			final MimeMessageParser mmp = new MimeMessageParser((MimeMessage) getMailMessage(m));
			mmp.parse();
			if (mmp.hasHtmlContent()) {
				return mmp.getHtmlContent();
			} else if (mmp.hasPlainContent()) {
				return mmp.getPlainContent();
			}
			return "Could not parse message's contents";
		} catch (Exception e) {
			logger.error("unable to parse MimeMessage ",e);
			return "Unable to display";
		}
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
