package com.paragonict.webapp.threader.services.impl;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.mail.FetchProfile;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.MailDateFormat;

import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.LoggerSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.slf4j.Logger;

import com.paragonict.webapp.threader.Utils;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailFetcher;
import com.paragonict.webapp.threader.services.IMailStore;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.pop3.POP3Folder;

public class MailFetcherImpl implements IMailFetcher {
	
	@Inject
	private HibernateSessionManager hsm;
	
	@Inject
	private IAccountService as;
	
	@Inject
	private IMailStore store;
	
	@Inject
	private LoggerSource logSource;
	
	
	// can we maintain a map of (completed) futures? this way we know when the executor is still running
	// and possibly wait for enough local messages 
	//private Map<FetcherKey,Future<Boolean>> futureHolder;
	
	private Logger logger;
	
	private MailDateFormat mdf;
	
	@PostInjection
	public void init() {
		logger = logSource.getLogger(this.getClass());
		//futureHolder = new ConcurrentHashMap<>();
		
		mdf = new MailDateFormat();
		
	}
	
	

	@Override
	public void isInSync(final String folder,final int start, final int end, int nrOfMsgsOnServer) {
		
		final Session hSession = hsm.getSession();
		hSession.setCacheMode(CacheMode.IGNORE);
		
		try {
    		final List<String> UIDs = hSession.getNamedQuery(LocalMessage.GET_ALL_UIDS).
    				setLong("accountid", as.getAccountID()).
    				setString("folder",folder).list();
        	
    		int nrofDBRows = UIDs.size();
    		logger.info("nr of db rows {}", nrofDBRows);
    		
    		if (nrofDBRows < nrOfMsgsOnServer) {
    			// just fetch the difference?
				javax.mail.Folder f = store.getFolder(folder);
				
				
				logger.info("Fetching from mail server nr {} to {}", start+1,end+1);
				final Message[] msgs = f.getMessages(start+1,end+1);
				final FetchProfile fp = new FetchProfile();
				fp.add(javax.mail.UIDFolder.FetchProfileItem.UID);
				//Implementations should include the following attributes: From, To, Cc, Bcc, ReplyTo, Subject and Date. More items may be included as well.
				//fp.add(FetchProfile.Item.ENVELOPE);
				f.fetch(msgs, fp);
				
				logger.info("msgs fetched!");
				// the order of msgs is completely undetermined (
				// maybe with IMAP server sort this can be done in another way (!?)
				final Long accountId = as.getAccountID();
				// process the messagelist in parallel and process every message
				
				/*
				 * FUU!
				 * TL;DR: SessionFactory is thread-safe, Session is not. Provide one new Session for each thread 
				 * (either by do it yourself or utilizing a DI framework)
				 */
				for (Message m :msgs) {
	// determine UID of currentMessage
					// filter the message we already have
					try {
						
						String UID;
						if (m.getFolder() instanceof POP3Folder) {
							UID = ((POP3Folder)m.getFolder()).getUID(m);
						} else {
							UID = "" + ((IMAPFolder)m.getFolder()).getUID(m);
						}
						if (UIDs.contains(UID)) {
							logger.debug("Message with UID {} already stored, skipping",UID);
						} else {
							// ah new mssg
							LocalMessage possiblyNewMsg = createLocalMessage(m, UID,accountId);
							logger.debug("Adding new localmessage with uid {} with date {}",UID,possiblyNewMsg.getReceivedDate());
							//create short lived transaction.. its NOT threadSAFE
							
							hSession.save(possiblyNewMsg);	
						}
						UIDs.remove(UID); // remove from list	
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				};
				hsm.commit();
    		}
    		/**
	 		final FetcherKey fk = new FetcherKey(folder, as.getAccountID());
    		
    		Future<Boolean> futureFetcher = null;
    		
    		if (futureHolder.containsKey(fk)) {
    			futureFetcher = futureHolder.get(fk);
    			if (futureFetcher.isCancelled() || futureFetcher.isDone()) {
    				// possibly an old fetcher done with work.
    				futureHolder.remove(fk);
    				futureFetcher = null;
    			} else {
    				logger.info("Future {} still running!", futureFetcher);
    			}
    		}
    		if (futureFetcher == null) {
    			// create and start the fetching process
    			futureFetcher = executor.invoke(new AsynchronousMailFetcher(tc,hss.create(),store.getUnmanagedStore(),fk,UIDs,nrOfMsgsOnServer));
				futureHolder.put(fk, futureFetcher);
    		} else {
    			logger.warn("Returning (running) future fetcher for account {} ",as.getAccountID());
    		}
    		// if future is running. wait for it or query the number of dbrows to see if there are enough msgs
    		
    		// keep querying the DB for the total number of rows 
    		// TODO limit waiting for like 5 minutes or something.
    		// then kill the future, throw exception and let the system retry
    		
    			
    		while ( nrofDBRows < end+1 && !futureFetcher.isDone() && !futureFetcher.isCancelled() ) {
				Thread.sleep(100); // sleep 100 ms
    			// the db is updated in another session, this one need to be cleared so we do a real DB query and not fetch cached info.
				//s.clear();
    			nrofDBRows = ((Long) s.getNamedQuery(LocalMessage.COUNT_ALL).
    					setLong("accountid", as.getAccountID()).
    					setString("folder",folder).uniqueResult()).intValue();
    		}
    		if (futureFetcher.isDone() || futureFetcher.isCancelled()) {
    			if (futureHolder.remove(fk) != null) {
    				logger.debug("Future removed for {}", fk);
    			};
    		} else {
    			logger.info("Nr of local messages sufficient, future {} is still running?!",futureFetcher);
    		}
    		s.close();
    		logger.debug("Fetcher is done, nr of local messages is now: {}",nrofDBRows);
    		**/
    		
    		
    		
    		
		} catch (Exception e ) {
			System.err.println(" we are fucked... " + e.getMessage());
			e.printStackTrace();
		}
 	}

	private LocalMessage createLocalMessage(Message message, String UID,Long accountId) throws MessagingException {
		final LocalMessage lm = new LocalMessage();
		
		Date sentDate = null;
		Date receivedDate = null;
		
		if (message.getFolder() instanceof POP3Folder) {
			
			// try to get date from pop3 headers
			/* works for :
			 * 
			 * - Hotmail
			 * 
			 * 
			 * 
			 */
			
			
			for (String str: message.getHeader("Date")) {
				try {
					receivedDate = mdf.parse(str);
					break;
				} catch (ParseException pe) {
					
				}
			}
			System.err.println(" sentDAte" + sentDate);
			
			if (receivedDate == null) {
				receivedDate = new Date();// just assume now
			}
			
			System.err.println(" recDAte" + receivedDate);
			
		} else {
			sentDate = message.getSentDate();
			receivedDate = message.getReceivedDate();
		}
		lm.setSentDate(sentDate);
		lm.setReceivedDate(receivedDate);
		
		lm.setFolder( message.getFolder().getFullName());
		lm.setFromAdr(Utils.toString(message.getFrom()));
		lm.setToAdr(Utils.toString(message.getRecipients(RecipientType.TO)[0]));
		lm.setSubject(message.getSubject());
		// probably better to save ALL flag states locally...
		lm.setMessageRead(message.isSet(Flag.SEEN));
		lm.setAccount(accountId);
		lm.setUID(UID);
		return lm;
	}
	
	
}
