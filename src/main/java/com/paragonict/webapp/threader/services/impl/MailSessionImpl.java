package com.paragonict.webapp.threader.services.impl;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.apache.tapestry5.ioc.services.ThreadCleanupListener;

import com.paragonict.webapp.threader.Constants;
import com.paragonict.webapp.threader.entities.Account;
import com.paragonict.webapp.threader.mail.MailStore;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailSession;

/**
 * Creates a mail session per thread (request) based on the logged-in account and closes the session after the thread ends.
 * 
 * @author avankalleveen
 *
 */
public class MailSessionImpl implements IMailSession, ThreadCleanupListener{

	private Properties systemProperties  = System.getProperties();
	
	// the store for the current user
	private MailStore _mailStore;
	
	private Session _mailsession; // a session to connecto to the store to retrieve mail
		
	@Inject
	private IAccountService as;
	
	@Inject
	@Symbol(value=Constants.SYMBOL_MAIL_DEBUG)
	private boolean debug;
	
	@PostInjection
	public void init(PerthreadManager pm) throws MessagingException {
		_mailStore = null;
		if (as.isLoggedIn()) {
			final Account currentAccount = as.getAccount();
			
			Properties props = systemProperties;
			setProperties(props);
			
			try {
				_mailsession =  Session.getInstance(props,new Authenticator() {
		        	@Override
		        	protected PasswordAuthentication getPasswordAuthentication() {
		        		return new PasswordAuthentication(as.getAccount().getEmailAddress(), as.getAccount().getPassword());
		        	}
				});
				if (debug)
					_mailsession.setDebug(true);
			    final Store store = _mailsession.getStore(currentAccount.getProtocol().name());
			    store.connect(); 
			    _mailStore = new MailStore(store);
			    pm.addThreadCleanupListener(this);
			} catch (Exception e) {
				_mailStore = null;
				_mailsession = null;
				throw new MessagingException("Could not connect to mailstore for current user due to :" + e.getMessage());
			}
		}
		
	}
	
	public MailStore getStore() throws MessagingException {
		if (_mailStore == null) {
			throw new MessagingException("No active mailstore, user not logged in or no connection!");
		}
		return _mailStore;
	}
	
	@Override
	public Session getSession() {
		return _mailsession;
	}
	
	public void threadDidCleanup() {
		if (_mailStore !=null) {
			_mailStore.closeMailStore();
			_mailsession = null;
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
