package com.paragonict.webapp.threader.pages;


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
		System.err.println("Report exception!" + exception);
		// clear session storage (except the logged in user)..
		sso.clearValue(SESSION_ATTRS.SELECTED_FOLDER);
		sso.clearValue(SESSION_ATTRS.SELECTED_MSG_ID);
		rootexception = exception;
	}
	
	public String getCause() {
		Throwable e = rootexception;
		while (e.getCause() != null) {
			e = e.getCause();
			
		}
		System.err.println(e.getMessage());
		return e.getMessage();
	}
	
}
