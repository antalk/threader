package com.paragonict.webapp.threader.components;


import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.pages.Index;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailSession;

/**
 * Layout component for pages of application threader.
 */
@Import(stack="bootstrap",stylesheet={"context:css/layout.css"},library="context:js/Threader.js")
public class Layout {
 
	@SessionState
	private SessionStateObject sso;
	
	@Inject
	private ComponentResources res;
	
	@Inject 
	private IAccountService as;
	
	@Inject
	private IMailSession session;
	
	@OnEvent(value="logout")
	private Object logout() {
		sso.clearAll();
		session.clearSession(as);
		return Index.class;
	}
	
	@Cached
	public boolean getIsLoggedIn() {
		return as.isLoggedIn();
	}
	
	@Cached
	public String getLinkEnabled() {
		return (as.isLoggedIn()?"enabled":"disabled");
	}
	
	public String getAccountName() {
		if (getIsLoggedIn()) {
			return as.getAccount().getFullName();
		} else {
			return "Account";
		}
	}
	
	
	public String getTitle() {
		return res.getMessages().get(res.getPage().getClass().getSimpleName()+".title");
	}
}
