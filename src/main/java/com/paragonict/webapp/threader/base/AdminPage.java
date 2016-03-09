package com.paragonict.webapp.threader.base;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.pages.admin.Index;

/*
 * Pages on accesible by admins
 * 
 */
public abstract class AdminPage extends BasicPage {
	
	public Object onActivate() {
		if (!getSso().hasValue(SESSION_ATTRS.ADMIN_ID)) {
			
			if (!getResources().getPage().getClass().equals(Index.class)) {
				return getPrls().createPageRenderLink(Index.class);// not logged in ? go to admin index, will show login popup
			}
		}
		//continue as is
		return null;
	}
	
}
