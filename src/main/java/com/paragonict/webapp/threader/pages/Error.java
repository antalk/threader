package com.paragonict.webapp.threader.pages;


import javax.mail.MessagingException;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.ExceptionReporter;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;

public class Error implements ExceptionReporter  {
	
	@Inject
	@Symbol(SymbolConstants.PRODUCTION_MODE)
	@Property(write = false)
	private boolean productionMode;
	
	@SessionState
	private SessionStateObject sso;

	@Property
	private Throwable rootexception;
	

	@Override
	public void reportException(Throwable exception) {
		// clear session storage (except the logged in user)..
		sso.clearValue(SESSION_ATTRS.SELECTED_FOLDER,SESSION_ATTRS.SELECTED_MSG_UID);
		rootexception = exception;
	}
	
	public String getCause() {
		Throwable e = rootexception;
		
		final MessagingException me = org.apache.tapestry5.ioc.util.ExceptionUtils.findCause(e, MessagingException.class);
		if (me !=null) {
			return "An communication error occured: ["+ e.getMessage()+"]. Please retry or re-configure your email account";
		}
		while (e.getCause() != null) {
			e = e.getCause();
		}
		return "An unhandled exception occured : " + e.getMessage();
	}
	
}
