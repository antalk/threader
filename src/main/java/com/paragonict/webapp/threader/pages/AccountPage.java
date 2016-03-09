package com.paragonict.webapp.threader.pages;

import javax.servlet.http.HttpServletRequest;

import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

// anonymous page
@Import(stack="Wizard",library="context:js/Wizard.js")
public class AccountPage {

	@Inject
	private JavaScriptSupport js;
	
	@Inject
	private HttpServletRequest req; 
	
	@OnEvent(value="accountSuccess")
	private Object getIndexPage() {
		return Index.class;
	}
	
	@SetupRender
	private void initPage() {
		js.addScript("progressBar()", new Object[]{});
	 }
}
