package com.paragonict.webapp.threader.pages;


import org.apache.tapestry5.Block;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.ajax.JavaScriptCallback;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.hibernate.criterion.Restrictions;

import com.paragonict.webapp.threader.ApplicationEvents;
import com.paragonict.webapp.threader.base.AppPage;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.Account;
import com.paragonict.webapp.threader.entities.Folder;

/**
 * Start page of application threader.
 */
public class Index extends AppPage {
	
	@Inject
	private Request req;
	
	@Inject
	private AlertManager am;
	
	@Component
	private Zone folderZone;
	
	@Component
	private Zone messageZone;
	
	@Component
	private Zone contentZone;

	@Component
	private Form loginForm;
	
	@Property
	private String mail;
	
	@Property
	private String password;
	
	@OnEvent(value=ApplicationEvents.MESSAGING_EXCEPTION_EVENT)
	private Object catchAndDisplayException(Exception e) {
		return null;
	}
	
	@OnEvent(component="loginform",value=EventConstants.SUBMIT)
	private void loginUser() {
		System.err.println("login with" + mail);
		
		Account user = (Account) getHsm().getSession().createCriteria(Account.class).add(Restrictions.eq("emailAddress", mail)).uniqueResult();
		if (user != null) {
			getSso().putValue(SESSION_ATTRS.USER_ID, user.getId());
		} else {
			loginForm.recordError("No such user");
		}
	}
	
	@OnEvent(value="createAccount")
	private Object getAccountPage() {
		return getPrls().createPageRenderLink(AccountPage.class);
	}

	@OnEvent(value="getFolderContent")
	private void getFolderContents(final Long id) {
		final Folder selectedFolder = (Folder) getHsm().getSession().load(Folder.class, id);
		getSso().putValue(SESSION_ATTRS.SELECTED_FOLDER, selectedFolder.getName());
		getArr().addRender(messageZone).addCallback(new JavaScriptCallback() {
			
			@Override
			public void run(JavaScriptSupport javascriptSupport) {
				javascriptSupport.addScript("selectFolder(%s);",new Object[] {selectedFolder.getId()});
			}
		});
	}
	
	@OnEvent(value="getMessageContent")
	private Block getMessageContents(final Integer id) {
		getSso().putValue(SESSION_ATTRS.SELECTED_MSG_ID, id);
		return contentZone.getBody();
	}
	
	@OnEvent(value="clearFolderZone")
	private void reloadFolders() { 
		getSso().clearValue(SESSION_ATTRS.SELECTED_FOLDER);
		getSso().clearValue(SESSION_ATTRS.SELECTED_MSG_ID);
		getArr().addRender(folderZone).addRender(messageZone).addRender(contentZone);
	}
	
	
	@OnEvent(value="reloadMessages")
	private void reloadMessages() {
		getArr().addRender(messageZone);
	}
	
	@OnEvent(value="reloadContent")
	private void reloadContent() {
		getArr().addRender(messageZone).addRender(contentZone);
	}
	
	public Block getActiveBlock() {
		if (getAs().isLoggedIn()) {
			return getResources().getBlock("main");
		}
		return getResources().getBlock("loginblock");
	}
		
}
