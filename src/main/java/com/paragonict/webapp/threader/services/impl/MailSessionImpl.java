package com.paragonict.webapp.threader.services.impl;

import javax.mail.Session;

import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.annotations.Symbol;

import com.paragonict.webapp.threader.Constants;
import com.paragonict.webapp.threader.services.IMailSession;

public class MailSessionImpl implements IMailSession {
	
	@Inject
	@Symbol(value=Constants.SYMBOL_MAIL_DEBUG)
	private boolean debug;
	
	
	private Session _mailSession;
	
	@PostInjection
	public void init() {
		// creates a singleton session for all
		_mailSession =  Session.getInstance(System.getProperties());
		if (debug) _mailSession.setDebug(true);
	}
	
	
	@Override
	public Session getSession() {
		return _mailSession;
	}
}
