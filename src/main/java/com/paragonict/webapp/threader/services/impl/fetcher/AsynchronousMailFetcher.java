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
	private final TypeCoercer tc;
	private final int nrOfMsgsOnServer;
	
	public AsynchronousMailFetcher(final TypeCoercer tc,final Session session,final Store store, final FetcherKey key,List<String> UIDs, int nrOfMsgsOnServer) {
		this.session = session;
		this.store = store;
		this.key = key;
		this.UIDs = UIDs;
		this.tc = tc;
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
				
				LocalMessage possiblyNewMsg = null;
					
				List<Message> workingList = Arrays.asList(msgs);
				// use a counter
				Transaction tx;
				for (int i=0;i<msgs.length;i++) {
					currentMessage = workingList.get(i);
				
					// determine UID of currentMessage
					// filter the message we already have
					possiblyNewMsg = new LocalMessage(tc,currentMessage, key.getAccountId());
					
					if (UIDs.contains(possiblyNewMsg.getUID())) {
						logger.debug("Message with UID {} already stored, skipping",possiblyNewMsg.getUID());
					} else {
						// ah new mssg
						logger.debug("Adding new localmessage from {} ",currentMessage);
						//create short lived transaction
						tx = session.beginTransaction();
						session.persist(possiblyNewMsg);
						tx.commit();
						nrofDBRows++;
					}
					UIDs.remove(possiblyNewMsg.getUID()); // remove from list

					if (nrOfMsgsOnServer <= nrofDBRows) {
						logger.info("Local number of messages {} equals the number of messages on server {}, skipping fetch at pos {}.",nrofDBRows,nrOfMsgsOnServer,i);
						break;
					}
					
				}
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
}
