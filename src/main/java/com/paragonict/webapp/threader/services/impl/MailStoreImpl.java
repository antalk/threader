package com.paragonict.webapp.threader.services.impl;

import java.util.HashMap;
import java.util.Map;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;

import org.apache.tapestry5.ioc.LoggerSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.apache.tapestry5.ioc.services.ThreadCleanupListener;
import org.slf4j.Logger;

import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailSession;
import com.paragonict.webapp.threader.services.IMailStore;

/**
 * Creates a mail session per thread (request) based on the logged-in account and closes the store after the thread ends. 
 * (in a different thread) 
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

	private Logger logger;
	
	private Map<String,Folder> requestedFolders;
		
	private Long startTime;
	
	@PostInjection
	public void init(PerthreadManager pm) throws MessagingException {
		logger = logSource.getLogger(MailStoreImpl.class);
		logger.debug("Opening mailstore at {} ", startTime=System.currentTimeMillis());
		pm.addThreadCleanupListener(this);
		// TODO: see if we can merge requested and cachefolders or detect duplicates.
		requestedFolders = new HashMap<String,Folder>(5); // start with 5..
		//cachedMessageFolders = new ArrayList<Folder>(5);

	}
	
	private synchronized void getStore() throws MessagingException {
		if (_delegate == null) {
			logger.debug("Createing new store for thread {}",Thread.currentThread().getId());
			_delegate = session.getSession(as).getStore(as.getAccount().getProtocol().name());
			_delegate.connect(as.getAccount().getHost(),as.getAccount().getAccountName(),as.getAccount().getPassword());
			logger.debug("Created new store {} for this thread {}",_delegate,Thread.currentThread().getId());
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
			final Folder f = _delegate.getFolder(name);
			if (!f.isOpen()) {
				f.open(Folder.READ_WRITE);
			}
			requestedFolders.put(name,f );
		} 
		return requestedFolders.get(name); // return the latest..
	}
	
	@Override
	public void registerFolder(Folder folder)throws MessagingException {
		if (!folder.isOpen()) {
			folder.open(Folder.READ_WRITE);
		}
		if (!requestedFolders.containsKey(folder.getName())) {
			requestedFolders.put(folder.getName(), folder);
		}
			
	}
	
	public void threadDidCleanup() {
		logger.debug("Closing mailstore {} at end of thread {}",_delegate,Thread.currentThread().getId());
		
		if (_delegate != null) {
			
			requestedFolders.values().stream().filter(f -> f.isOpen()).forEach(f -> {
				logger.debug("Closing folder {}", f);
				try {
					f.close(true);
				} catch (MessagingException e) {
					logger.error("Could not close folder due to {}",e.getMessage(),e);
				} // expunge true..
			});
			
			try {
				if (_delegate.isConnected()) {
					_delegate.close();
				}
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
