package com.paragonict.webapp.threader.pages;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.Block;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import com.paragonict.webapp.threader.base.BasicPage;

// anonymous page
@Import(stack="Wizard",library="context:js/Wizard.js")
public class AccountPage extends BasicPage {

	@Inject
	private JavaScriptSupport js;
	
	@Inject
	private HttpServletRequest req; 
	
	@Property
	@Persist(PersistenceConstants.SESSION)
	private String action;
	
    @Override
    public Object handleEventContext(EventContext context) {
    	if (context.getCount() == 0 ) {
			if (StringUtils.isBlank(action)) {
				return getIndexPage();
			}
 		} else {
			action = context.get(String.class, 0);
		}
		return null;
	}
	
	public Block getActiveBlock() {
		return getResources().findBlock(action);
	}
	
	
	@OnEvent(value="accountSuccess")
	private Object getIndexPage() {
		return Index.class;
	}
	
	
	
	
	@SetupRender
	private void initPage() {
		System.err.println("accountpage setuprender");
		js.addScript("progressBar()", new Object[]{});
	 }
}
