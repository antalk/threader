package com.paragonict.webapp.threader.services.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.annotations.Symbol;

import com.paragonict.webapp.threader.Constants;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailSession;

public class MailSessionImpl implements IMailSession {
	
	@Inject
	@Symbol(value=Constants.SYMBOL_MAIL_DEBUG)
	private boolean debug;
	
	private Map<Long,Session> _mailSessions;

	@PostInjection
	public void init() {
		_mailSessions = new HashMap<>();
	}
	
	@Override
	public synchronized Session getSession(final IAccountService as) {
		if (!_mailSessions.containsKey(as.getAccountID())) {
			
			System.err.println("Creating new mail Session for user " + as);
			
			final Session userSession = Session.getInstance(setProperties(as));
			if (debug) userSession.setDebug(true);
			_mailSessions.put(as.getAccountID(), userSession);
		}
		System.err.println("returning existing session for user");
		
		return _mailSessions.get(as.getAccountID());
	}
	
	@Override
	public void clearSession(IAccountService as) {
		Session s = _mailSessions.remove(as.getAccountID());
		try {
			s.getStore().close();
		} catch (MessagingException e) {
			
		}
		s = null;
	}
	
	private Properties setProperties(final IAccountService as) {
		
		final Properties props = System.getProperties();
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
			case pop3s:
				props.setProperty("mail.store.protocol", as.getAccount().getProtocol().name());
			    props.setProperty("mail.pop3.ssl.enable", "true");

				break;
			default:
				break;
		}
		return props;
	}
	 
}
