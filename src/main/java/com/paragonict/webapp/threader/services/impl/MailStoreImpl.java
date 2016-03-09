package com.paragonict.webapp.threader.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.tapestry5.ioc.LoggerSource;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.apache.tapestry5.ioc.services.ThreadCleanupListener;
import org.slf4j.Logger;

import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailService;
import com.paragonict.webapp.threader.services.IMailStore;

/**
 * Creates a mail session per thread (request) based on the logged-in account and closes the session after the thread ends.
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
	private IMailService ms;
	
	@Inject
	private LoggerSource logSource;
	
	private Logger logger;
	
	private Map<String,Folder> requestedFolders;
	
	private List<Folder> cachedMessageFolders;
	
	@PostInjection
	public void init(PerthreadManager pm) throws MessagingException {
		pm.addThreadCleanupListener(this);
		logger = logSource.getLogger(MailStoreImpl.class);
		// TODO: see if we can merge requested and cachefolders or detect duplicates.
		requestedFolders = new HashMap<String,Folder>(5); // start with 5..
		cachedMessageFolders = new ArrayList<Folder>(5);

	}
	
	public IMailStore getStore(final Session session) throws MessagingException {
		if (_delegate == null) {
			_delegate = session.getStore(as.getAccount().getProtocol().name());
			_delegate.connect(as.getAccount().getHost(),as.getAccount().getAccountName(),as.getAccount().getPassword());
		}
		return this;
	}
	
	public Folder getDefaultFolder() throws MessagingException {
		return _delegate.getDefaultFolder();
	}

	public Folder getFolder(String name) throws MessagingException {
		if (!requestedFolders.containsKey(name)) {
			requestedFolders.put(name, _delegate.getFolder(name));
		}
		return requestedFolders.get(name); // return the latest..
	}
	
	public Folder[] getPersonalNamespaces() throws MessagingException {
		return _delegate.getPersonalNamespaces();
	}
	
	public Folder[] getSharedNamespaces() throws MessagingException {
		return _delegate.getSharedNamespaces();
	}
	
	public Folder[] getUserNamespaces(String user) throws MessagingException {
		return _delegate.getUserNamespaces(user);
	}
	
	@Override
	public void registerFolder(Folder folder) {
		cachedMessageFolders.add(folder);
	}

	public void threadDidCleanup() {
		if (logger.isDebugEnabled()) {
			logger.debug("Closing mailstore at end of request");
		}
		try {
			for (Folder f: requestedFolders.values()) {
				if (f.isOpen()) {
					System.out.println("EOR Closing folder :" + f);
					f.close(true); // expunge true...
				}
			}
			for (Folder f: cachedMessageFolders) {
				if (f.isOpen()) {
					System.out.println("EOR Closing (cached) folder :" + f);
					f.close(true); // expunge true...
				}
			}
			requestedFolders = null;
			cachedMessageFolders = null;
			if (_delegate != null) {
				if (_delegate.isConnected()) {
					System.out.println("EOR Closing mailStore :");
					_delegate.close();
				}
			}
			
		} catch (MessagingException me) {
			System.err.println("Error closing mailStore: " + me.getMessage());
		}
	}
	
	private void setProperties(final Properties props) {
		
		props.put("mail.transport.protocol","smtp"); 
		props.put("mail.smtp.auth", as.getAccount().getSmtpAuth().toString());
		props.put("mail.smtp.host", as.getAccount().getSmtpHost());
		props.put("mail.smtp.port", as.getAccount().getSmtpPort());
		props.put("mail.smtp.starttls.enable", as.getAccount().getSmtpTLS().toString());
		
		props.put("mail."+as.getAccount().getProtocol().name()+".host", as.getAccount().getHost());
		
		switch (as.getAccount().getProtocol()) {
			case imap:
				props.setProperty("mail.store.protocol", as.getAccount().getProtocol().name());
				break;
			case imaps:
				props.setProperty("mail.store.protocol", as.getAccount().getProtocol().name());
				props.setProperty("mail.imap.starttls.enable", "true");
			    props.setProperty("mail.imap.ssl.enable", "true");
				break;
			case pop3:
				props.setProperty("mail.store.protocol", as.getAccount().getProtocol().name());
				break;
			case pops:
				props.setProperty("mail.store.protocol", as.getAccount().getProtocol().name());
				break;
			default:
				break;
		}
	}

}
