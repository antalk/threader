package com.paragonict.webapp.threader.base;


import org.apache.tapestry5.ioc.annotations.Inject;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.pages.Index;
import com.paragonict.webapp.threader.services.IAccountService;

public abstract class AuthenticatedPage extends BasicPage {
	
	@Inject
	private IAccountService as;
	
	public Object onActivate() {
		if (!getSso().hasValue(SESSION_ATTRS.USER_ID)) {
			
			if (!getResources().getPage().getClass().equals(Index.class)) {
				return getPrls().createPageRenderLink(Index.class);// not logged in ? go to index, will show login popup
			}
		}
		//continue as is
		return null;
	}
	
	public IAccountService getAs() {
		return as;
	}
	
	
}
