package com.paragonict.webapp.threader.components;

import java.io.IOException;

import javax.mail.Flags.Flag;
import javax.mail.MessagingException;

import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.paragonict.webapp.threader.annotation.RequiresLogin;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailService;

@RequiresLogin
public class Contents {

	@SessionState
	private SessionStateObject sso;
	
	@Inject
	private IMailService ms;
	
	@Inject
	private IAccountService as;
	
	@Inject
	private HibernateSessionManager hsm;
	
	@Inject
	private ComponentResources resources;
	
	public boolean getMessageSelected() {
		return (sso.hasValue(SESSION_ATTRS.SELECTED_MSG_UID) &&
				sso.hasValue(SESSION_ATTRS.SELECTED_FOLDER));
	}
	
	
	@Cached
	public LocalMessage getMessage()  throws MessagingException {
		if (getMessageSelected()) {
			return ms.getLocalMessage(sso.getStringValue(SESSION_ATTRS.SELECTED_MSG_UID));
		}
		throw new MessagingException("Cannot retrieve selected message");
	}
	
	public String getMessageContent() throws MessagingException, IOException {
		return (String) ms.getMailMessage(getMessage()).getContent();
	}
	
	
	@OnEvent(value=EventConstants.PROGRESSIVE_DISPLAY)
	private Block loadMessages() throws MessagingException {
		ms.getMailMessage(getMessage()).setFlag(Flag.SEEN, true);
		return resources.getBlock("contentblock");
	}
	
	@OnEvent(value="markasunread")
	private Block markMessageAsUnread() throws MessagingException {
		ms.getMailMessage(getMessage()).setFlag(Flag.SEEN, false);
		return resources.getBlock("contentblock");
	}
	
	@OnEvent(value="deletemessage")
	private void deleteMessage() throws MessagingException {
		//getMessage().setFlag(Flag.DELETED,true);
		sso.clearValue(SESSION_ATTRS.SELECTED_MSG_UID);
		resources.triggerEvent("reloadContent", new Object[]{}, null);
	}
	
	@OnEvent(value="viewmessage")
	private Block openMessage() {
		return resources.getBlock("messageViewBlock");
	}
	
	@OnEvent(value="reply")
	private Block replyToMessage() throws MessagingException, IOException {
		final com.paragonict.webapp.threader.entities.LocalMessage newMessage = new com.paragonict.webapp.threader.entities.LocalMessage();
		newMessage.setAccount(as.getAccount().getId());
		newMessage.setFromAdr(as.getAccount().getEmailAddress());
		newMessage.setToAdr(getMessage().getFromAdr());
		
		newMessage.setSubject(resources.getMessages().get("reply.prefix") + getMessage().getSubject());
		//newMessage.setFolder(getMessage().getFolder().getFullName());
		//newMessage.setMsgid(getMessage().getMessageNumber());
		
		hsm.getSession().saveOrUpdate(newMessage);
		hsm.commit();
		return resources.getBlock("composeBlock");
	}
	
	public boolean getMessageRead() throws MessagingException {
		return ms.getMailMessage(getMessage()).getFlags().contains(Flag.SEEN);
	}
	
	
}
