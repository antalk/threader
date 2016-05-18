package com.paragonict.webapp.threader.pages;


import java.io.UnsupportedEncodingException;

import javax.mail.MessagingException;

import org.apache.tapestry5.Block;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ajax.JavaScriptCallback;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.hibernate.criterion.Restrictions;

import com.paragonict.webapp.threader.base.AuthenticatedPage;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.Account;
import com.paragonict.webapp.threader.entities.Folder;
import com.paragonict.webapp.threader.services.IMailStore;

/**
 * Start page of application threader.
 */
public class Index extends AuthenticatedPage {
	
	@Inject
	private IMailStore ms;
	
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
	
	Object onException(Throwable cause) {
		
		final MessagingException me = org.apache.tapestry5.ioc.util.ExceptionUtils.findCause(cause, MessagingException.class);
		if (me !=null) {
			//getLogger().error("MessagingException",cause);
			//am.error("An communication error occured: ["+ cause.getMessage()+"]. Please retry or re-configure your email account");
			//return getPrls().createPageRenderLink(Index.class);
		}
		return null;
	}
	
	@OnEvent(component="loginform",value=EventConstants.SUBMIT)
	private void loginUser() {
		getSso().clearValue(SESSION_ATTRS.USER_ID);
		System.err.println("login with" + mail + "and pw: " +password);
		
		final Account user = (Account) getHsm().getSession().createCriteria(Account.class).add(Restrictions.eq("emailAddress", mail)).uniqueResult();
		if (user != null) {
			if (user.getPassword().equals(password)) {
				getSso().putValue(SESSION_ATTRS.USER_ID, user.getId());
			} else {
				loginForm.recordError("Wrong user and/or password");
			}
		} else {
			loginForm.recordError("Wrong user and/or password");
		}
	}
	
	
	
	@OnEvent(value="createAccount")
	private Object getAccountPage() {
		return getPrls().createPageRenderLinkWithContext(AccountPage.class,"create");
	}

	@OnEvent(value="getFolderContent")
	private void getFolderContents(final Long id) {
		final Folder selectedFolder = (Folder) getHsm().getSession().load(Folder.class, id);
		
		// refresh the nr of unread msgs ONLY if not a POP3 folder
		if (selectedFolder.getUnreadMsgs() != -1) {
			try {
				selectedFolder.setUnreadMsgs(ms.getFolder(selectedFolder.getName()).getUnreadMessageCount());
				getHsm().getSession().update(selectedFolder);
				getHsm().commit();
			} catch (MessagingException e) {
				// oops
				getLogger().warn("Unable to update nr of unread msgs for folder ["+selectedFolder+"] , due to "+ e.getMessage());
			}
		} else {
			selectedFolder.setUnreadMsgs(0);
		}
		getSso().putValue(SESSION_ATTRS.SELECTED_FOLDER, selectedFolder.getName());
		getSso().clearValue(SESSION_ATTRS.SELECTED_MSG_UID);
		getArr().addRender(messageZone).addRender(contentZone).addCallback(new JavaScriptCallback() {
			
			@Override
			public void run(JavaScriptSupport javascriptSupport) {
				javascriptSupport.addScript("selectFolder(%d,%d);",new Object[] {selectedFolder.getId(),selectedFolder.getUnreadMsgs()});
			}
		});
		
	}
	
	@OnEvent(value="composeMessage")
	private Block getMessageEditor(EventContext context) throws UnsupportedEncodingException {
		if (context.getCount() == 1) {
			String UID = context.get(String.class,0);
			getSso().putValue(SESSION_ATTRS.DRAFT_UID, UID);
		}
		return getResources().getBlock("composeBlock");
	}

	
	@OnEvent(value="getMessageContent")
	private Block getMessageContents(final String UID) {
		getSso().putValue(SESSION_ATTRS.SELECTED_MSG_UID, UID);
		return contentZone.getBody();
	}
	
	@OnEvent(value="clearFolderZone")
	private void reloadFolders() { 
		getSso().clearValue(SESSION_ATTRS.SELECTED_FOLDER);
		getSso().clearValue(SESSION_ATTRS.SELECTED_MSG_UID);
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
