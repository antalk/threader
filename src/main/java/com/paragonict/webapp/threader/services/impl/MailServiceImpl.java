package com.paragonict.webapp.threader.services.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import org.apache.tapestry5.grid.ColumnSort;
import org.apache.tapestry5.grid.SortConstraint;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.criterion.Restrictions;

import com.paragonict.webapp.threader.Utils;
import com.paragonict.webapp.threader.beans.ClientMessage;
import com.paragonict.webapp.threader.entities.Folder;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailCache;
import com.paragonict.webapp.threader.services.IMailService;
import com.paragonict.webapp.threader.services.IMailSession;
import com.sun.mail.imap.IMAPFolder.FetchProfileItem;
import com.sun.mail.imap.IMAPMessage;

/*
 * TODO: Wrap IMailCache into THIS
 * 
 * 
 */
public class MailServiceImpl implements IMailService {
	
	@Inject
	private HibernateSessionManager hsm;
	
	@Inject
	private IMailSession mailSession;
	
	@Inject
	private IMailCache cache;
	
	@Inject
	private IAccountService as;

	public Folder getFolders(boolean renew) throws MessagingException {
		Folder rootFolder;
		
		if (as.isLoggedIn()) {
			List<Folder> folderList = hsm.getSession().createCriteria(Folder.class).add(Restrictions.eq("account", as.getAccount())).list();
			if (folderList.isEmpty() || renew == true) {
				
				List<Folder> oldFolders= hsm.getSession().createCriteria(Folder.class).add(Restrictions.eq("account", as.getAccount())).list();
				for (Folder o :oldFolders){
					hsm.getSession().delete(o);
				}
				hsm.commit();
				
				//javax.mail.Folder[] folders = mailSession.getStore().getDefaultFolder().list("%");
				
				javax.mail.Folder root = mailSession.getStore().getDefaultFolder();
				System.err.println("parent :"+ root);
				
				rootFolder = new Folder();
				rootFolder.setAccount(as.getAccount());
				rootFolder.setName("root");
				hsm.getSession().persist(rootFolder);
				
				persistFolders(rootFolder,Arrays.asList(mailSession.getStore().getDefaultFolder().list("%")));
				hsm.commit();
			} else {
				rootFolder = (Folder) hsm.getSession().createCriteria(Folder.class).add(Restrictions.eq("name", "root")).add(Restrictions.eq("account", as.getAccount())).uniqueResult();
			}
			return rootFolder;
		} else {
			throw new MessagingException("User not logged in!");
		}
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
	
	
	public List<ClientMessage> getMessages(final String folder,final int start,final int end, final SortConstraint sc) throws MessagingException {
		final List<ClientMessage> clientMsgs = new LinkedList<ClientMessage>();
		if (folder != null) {
			
			System.err.println("GET MSGS" + System.currentTimeMillis());
			
			
			javax.mail.Folder f = mailSession.getStore().getFolder(folder);
			f.open(javax.mail.Folder.READ_ONLY);
			
			List<Message> fullList = new LinkedList<Message>();
			Message[] msgs;
			// differentiate between IMAP and POP3
			
			// TODO first figure out if SORT is supported as extension !??
			 // otherwise this crahes..
			/*
			if (f instanceof IMAPFolder) {
				
				
				((IMAPFolder)f).
				
				SortTerm sm;
				if (sc == null) {
					sm = SortTerm.ARRIVAL;
				} else {
					switch (sc.getPropertyModel().getPropertyName().toLowerCase()) {
						case  "from" :{
							sm = SortTerm.FROM;
							break;
						}
						case "subject" : {
							sm = SortTerm.SUBJECT;
							break;
						}
						case "sentdate" :{
							sm = SortTerm.ARRIVAL;
							break;
						}
						default : {
							sm = SortTerm.ARRIVAL;
						}
					}
				}
				// use this !
				msgs = ((IMAPFolder)f).getSortedMessages(new SortTerm[] {sm});
				fullList = Arrays.asList(msgs);
				// then reverse list if requested.
				if (sc!=null){
					if (sc.getColumnSort().compareTo(ColumnSort.DESCENDING) ==0 ) {
						Collections.reverse(fullList);
					}
				}
			} else {
			*/
				// the hard way
				// just get ALL msgs
				// fetch ENVELOPES...
				msgs = f.getMessages();
				javax.mail.FetchProfile fp = new javax.mail.FetchProfile();
				fp.add(FetchProfileItem.ENVELOPE); //from to, cc, bcc, date fields
				// cant sort on flags so why retriev?
				//fp.add(FetchProfileItem.FLAGS); //from to, cc, bcc, date fields
				f.fetch(msgs, fp);// Load the profile of the messages in 1 fetch. // ffuuu if over 1000 messages!!
				
				String sortField;
				boolean negate = false;
				if (sc!=null) {
					sortField = sc.getPropertyModel().getPropertyName().toLowerCase();
					negate = (sc.getColumnSort().compareTo(ColumnSort.DESCENDING) ==0);
				} else {
					sortField = "sentdate"; // default
				}
				MessageComparator mc = new MessageComparator(sortField);
				// sort the array
				Arrays.sort(msgs, mc);
				fullList = Arrays.asList(msgs);
				msgs = null;
				if (negate) {
					Collections.reverse(fullList);
				}
				
			//}
			// then, only select the messages in range!
			// check for OOB !!
			System.err.println("FULL LIST LENGTH" +fullList.size());
			System.err.println("sTART: " +start);
			System.err.println("END: " +end);
			fullList = fullList.subList(start, end+1);

			System.err.println("SUBLIST " + System.currentTimeMillis() + " size:" + fullList.size());
			// then wrap em into ClientMessages, only now a server round trip for flags is made. but it is only for 25 records.
			ClientMessage cm;
			for (Message m: fullList) {
				
				// then lookup UID of this message
				String UID = "";
				
				if (f instanceof com.sun.mail.pop3.POP3Folder) {
				    com.sun.mail.pop3.POP3Folder pf = (com.sun.mail.pop3.POP3Folder)f;
				    UID = pf.getUID(m);
				} else if (f instanceof com.sun.mail.imap.IMAPFolder) {
					UID = ""+((com.sun.mail.imap.IMAPFolder) f).getUID(m);
				}
				clientMsgs.add(cache.getMessage(UID, m));
			}
			fullList = null;
			System.err.println("END GET MSGS" + System.currentTimeMillis());
		}
		return clientMsgs;
	}
	
	
	@Override
	public Integer getNrOfMessages(String folder) throws MessagingException {
		if (folder != null) {
			javax.mail.Folder f  = mailSession.getStore().getFolder(folder);
			f.open(javax.mail.Folder.READ_ONLY);
			return f.getMessageCount();
		}
		return 0;
	}
	
	public ClientMessage getMessage(final String folder, final Integer id) throws MessagingException {
		if (id != null && folder != null) {
			javax.mail.Folder f = mailSession.getStore().getFolder(folder);
			f.open(javax.mail.Folder.READ_ONLY);
			
			// is this fast?
			final Message msg = f.getMessage(id);
			// then lookup UID of this message
			
			String UID = "";
			
			if (f instanceof com.sun.mail.pop3.POP3Folder) {
			    com.sun.mail.pop3.POP3Folder pf = (com.sun.mail.pop3.POP3Folder)f;
			    UID = pf.getUID(msg);
			} else if (f instanceof com.sun.mail.imap.IMAPFolder) {
				UID = ""+((com.sun.mail.imap.IMAPFolder) f).getUID(msg);
			}
			return cache.getMessage(UID, msg);
		}
		return null;
	}

	

	private class MessageComparator implements Comparator<Message> {
		
		private final String _field;
		
		public MessageComparator(String field) {
			_field = field;
		}
		
		@Override
		public int compare(Message o1, Message o2) {
			try {
				switch (_field) {
					case "from" : {
						return o1.getFrom()[0].toString().compareToIgnoreCase(o2.getFrom()[0].toString());
					}
					case "subject" : {
						return o1.getSubject().compareToIgnoreCase(o2.getSubject());
					}
					case "sentdate" : {
						return o1.getSentDate().compareTo(o2.getSentDate());
					}
					default : {
						break;
					}
				}
			} catch (MessagingException me) {
				me.printStackTrace();
			}
				
			return 0;
		}
	}
	
}
