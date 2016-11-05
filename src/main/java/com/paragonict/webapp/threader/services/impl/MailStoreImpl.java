package com.paragonict.webapp.threader.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;

import org.apache.commons.collections.ListUtils;
import org.apache.tapestry5.ioc.Invokable;
import org.apache.tapestry5.ioc.LoggerSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.services.ParallelExecutor;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.apache.tapestry5.ioc.services.ThreadCleanupListener;
import org.slf4j.Logger;

import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailSession;
import com.paragonict.webapp.threader.services.IMailStore;

/**
 * Creates a mail session per thread (request) based on the logged-in account and closes the session after the thread ends.
 * 
 * Its in the INTERNAL package, meaning NOT useable in other pages/components !!
 * 
 * @author avankalleveen
 *
 */
public class MailStoreImpl implements IMailStore, ThreadCleanupListener {
	
	// the store (delegate) for the current user, current thread
	private Store _delegate;
		
	@Inject
	private IAccountService as;
	
	@Inject
	private IMailSession session;
	
	@Inject
	private LoggerSource logSource;
	
	@Inject
	private ParallelExecutor executor;
	
	private Logger logger;
	
	private Map<String,Folder> requestedFolders;
	
	// cachedMessageFolders are folders openend for Locally stored messages (LocalMessage)
	private List<Folder> cachedMessageFolders;
	
	private Long startTime;
	
	@PostInjection
	public void init(PerthreadManager pm) throws MessagingException {
		logger = logSource.getLogger(MailStoreImpl.class);
		logger.debug("Opening mailstore at {} ", startTime=System.currentTimeMillis());
		pm.addThreadCleanupListener(this);
		// TODO: see if we can merge requested and cachefolders or detect duplicates.
		requestedFolders = new HashMap<String,Folder>(5); // start with 5..
		cachedMessageFolders = new ArrayList<Folder>(5);

	}
	
	private synchronized void getStore() throws MessagingException {
		if (_delegate == null) {
			_delegate = session.getSession(as).getStore(as.getAccount().getProtocol().name());
			logger.debug("Createing new store {} for thread {}",_delegate,Thread.currentThread().getId());
			_delegate.connect(as.getAccount().getHost(),as.getAccount().getAccountName(),as.getAccount().getPassword());
			logger.debug("Created new store (delegate) for this request");
		}
	}
	
	@Override
	public Store getUnmanagedStore() throws MessagingException {
		final Store store = session.getSession(as).getStore(as.getAccount().getProtocol().name());
		store.connect(as.getAccount().getHost(),as.getAccount().getAccountName(),as.getAccount().getPassword());
		return store;
		
	}
	
	public Folder getDefaultFolder() throws MessagingException {
		getStore(); // initializes the store if not alreayd there, could be done with a decorator!
		return _delegate.getDefaultFolder();
	}

	public Folder getFolder(String name) throws MessagingException {
		getStore();
		if (!requestedFolders.containsKey(name)) {
			requestedFolders.put(name, _delegate.getFolder(name));
		}
		return requestedFolders.get(name); // return the latest..
	}
	
	@Override
	public void registerFolder(Folder folder) {
		cachedMessageFolders.add(folder);
	}
	
	public void threadDidCleanup() {
		logger.debug("Closing mailstore {} at end of thread {}",_delegate,Thread.currentThread().getId());
		
		executor.invoke(new StoreCloserThread(_delegate, ListUtils.union(cachedMessageFolders, new ArrayList(requestedFolders.values()))));
		logger.debug("Time spend in mailstore is {} ", System.currentTimeMillis()-startTime);
	}
		
	private class StoreCloserThread implements Invokable<Boolean> {
		
		private final Store store;
		private final List<Folder> folders;
		
		public StoreCloserThread(final Store store,final List<Folder> foldersToClose) {
			this.store  = store;
			this.folders =foldersToClose;
		}

		@Override
		public Boolean invoke() {
			logger.debug("Closing store {} in different thread {}",store,Thread.currentThread().getId());
			if (store != null) {
				
				folders.stream().filter(f -> f.isOpen()).forEach(f -> {
					logger.debug("Closing folder {}", f);
					try {
						f.close(true);
					} catch (MessagingException e) {
						logger.error("Could not close folder due to {}",e.getMessage(),e);
					} // expunge true..
				});
				
				try {
					if (store.isConnected()) {
						store.close();
					}
				} catch (MessagingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// we can return false in case of errors.. ? 
			return true;
		}
		
	}
	
}
