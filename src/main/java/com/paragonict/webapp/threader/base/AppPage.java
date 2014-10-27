package com.paragonict.webapp.threader.base;


import org.apache.tapestry5.ioc.annotations.Inject;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.pages.Index;
import com.paragonict.webapp.threader.services.IAccountService;

public abstract class AppPage extends BasicPage {
	
	@Inject
	private IAccountService as;
	
	public Object onActivate() {
		if (getSso().getValue(SESSION_ATTRS.USER_ID) == null) {
			
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
