package com.paragonict.webapp.threader.components;


import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.pages.Index;

/**
 * Layout component for pages of application threader.
 */
@Import(stack="bootstrap",stylesheet={"context:css/layout.css"},library="context:js/Threader.js")
public class Layout {
 
	@SessionState
	private SessionStateObject sso;
	
	@Inject
	private ComponentResources res;
	
	@OnEvent(value="logout")
	private Object logout() {
		sso.clearAll();
		return Index.class;
	}
	
	public String getTitle() {
		return res.getMessages().get(res.getPage().getClass().getSimpleName()+".title");
	}
}
