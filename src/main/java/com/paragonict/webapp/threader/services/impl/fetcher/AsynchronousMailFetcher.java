package com.paragonict.webapp.threader.services.impl.fetcher;

import java.util.Arrays;
import java.util.List;

import javax.mail.FetchProfile;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

import org.apache.tapestry5.ioc.Invokable;
import org.apache.tapestry5.ioc.services.TypeCoercer;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paragonict.webapp.threader.entities.LocalMessage;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.pop3.POP3Folder;

/**
 * Autonomous mail fetcher, is run in separate thread
 * 
 * @author Antal
 *
 */
public class AsynchronousMailFetcher implements Invokable<Boolean> {
	
	private static final Logger logger = LoggerFactory.getLogger(AsynchronousMailFetcher.class);

	private final FetcherKey key;
	private final Store store;
	private final Session session;
	private final List<String> UIDs;
	private final int nrOfMsgsOnServer;
	
	public AsynchronousMailFetcher(final TypeCoercer tc,final Session session,final Store store, final FetcherKey key,List<String> UIDs, int nrOfMsgsOnServer) {
		this.session = session;
		this.store = store;
		this.key = key;
		this.UIDs = UIDs;
		this.nrOfMsgsOnServer = nrOfMsgsOnServer;
	}

	@Override
	public Boolean invoke() {
		Message currentMessage = null;
		
		try {
			// FETCH ALL LOCAL UIDS FROM the selected folder
			int nrofDBRows = UIDs.size();
			
	        logger.debug("Nr. of local stored messages {}, number of message on server {}", nrofDBRows,nrOfMsgsOnServer);
	        
	        // this runs if there are still more messages on server than locally.
	        
	        if (nrOfMsgsOnServer > nrofDBRows) {
	        	//we can just go ahead and fetch new messages... beacuse end nuber is based on mailTotal at max.
	        	
        		// this is also wrong the results coudl be the same but different message could be added / deleted from a 
	        	// remote client..... (with the same account.... though..)
	        	// maybe a possible solution is to periodically check for changes and update the local db as such...
	        	// or add some listeners to mailserver updates...
	        	
	        	javax.mail.Folder f = store.getFolder(key.getFolder());
				if (!f.isOpen()) {
					f.open(javax.mail.Folder.READ_ONLY); // read only
				} 
				
				
				final Message[] msgs = f.getMessages();
				final FetchProfile fp = new FetchProfile();
				fp.add(javax.mail.UIDFolder.FetchProfileItem.UID);
				f.fetch(msgs, fp);
				
				
				for (Message m: msgs) {
					System.err.println(" msg: date; " + m.getSubject());
				} 
				
				
				// the order is in which they arrived..
				
				final List<Message> workingList = Arrays.asList(msgs);
				// process the messagelist in order received and process every message
				workingList.stream().forEach(m -> {
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
							LocalMessage possiblyNewMsg = null;//moved createLocalMessage(m, UID,key.getAccountId());
							logger.debug("Adding new localmessage from {} with date {}",m,possiblyNewMsg.getReceivedDate());
							//create short lived transaction
							Transaction tx = session.beginTransaction();
							session.persist(possiblyNewMsg);
							tx.commit();
						}
						UIDs.remove(UID); // remove from list	
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});
				f.close(false);// close folder, dont expunge
	        }
		} catch(Exception e ) {
			System.err.println("[Async] AsynchronousFetcher gone wrong !!!" + e.getMessage());
			e.printStackTrace();
		} finally {
			if (logger.isDebugEnabled()) {
				logger.debug("AsyncFetcher done! Closing (hibernate)session, store and folder");
			}
			if (session.isOpen()) {
				session.close();
			}
			if (currentMessage !=null) {
				try {
					if (currentMessage.getFolder().isOpen()) {
						currentMessage.getFolder().close(false);
					}
				} catch (Exception e) {
					System.err.println("Could not close non managed folder from AsyncFetcher..");
				}
			}
			if (store != null) {
				if (store.isConnected()) {
					try {
						store.close();
					} catch (MessagingException e) {
						System.err.println("Error closing unmanaged store: "+ store);
						e.printStackTrace();
					}
				}
			}
		
		} 
		
		return true;
	}
	
	/**
    * Check whether the email store has the sort capability or not.
    *
    * @param store Email store
    * @return true if the store is an IMAP store and it has the store capability
    * @throws MessagingException In case capability check fails
    */
   private boolean hasSortCapability(Store store) throws MessagingException {
       if (store instanceof IMAPStore) {
           IMAPStore imapStore = (IMAPStore) store;
           if (imapStore.hasCapability("SORT*")) {
               return true;
           }
       }
       return false;
   }
   
   
}
