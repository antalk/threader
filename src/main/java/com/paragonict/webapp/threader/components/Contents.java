package com.paragonict.webapp.threader.components;

import java.io.IOException;
import java.util.UUID;

import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentEventCallback;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.internal.util.Holder;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.paragonict.webapp.threader.annotation.RequiresLogin;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IApplicationError;
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
	
	@Inject
	private IApplicationError appErrors;
	
	@Property
	private String messagecontent;
	
	public boolean getMessageSelected() {
		return (sso.hasValue(SESSION_ATTRS.SELECTED_MSG_UID) &&
				sso.hasValue(SESSION_ATTRS.SELECTED_FOLDER));
	}
	
	@Cached
	public LocalMessage getMessage()  throws MessagingException {
		if (getMessageSelected()) {
			// retrieve message
			LocalMessage lm = ms.getLocalMessage(sso.getStringValue(SESSION_ATTRS.SELECTED_MSG_UID));
			if (lm != null) {
				final Message m = ms.getMailMessage(lm);
				if (m==null) {
					lm = null;
					appErrors.addApplicationError("Message was not on the server anymore");
				}
				return lm;
			}
			
		}
		return null;
	}
	@OnEvent(value=EventConstants.PROGRESSIVE_DISPLAY,component="loadMessageContent")
	public void loadMessageContent() throws MessagingException, IOException {
		messagecontent = ms.getMessageContent(getMessage());
	}
	
	
	@OnEvent(value=EventConstants.PROGRESSIVE_DISPLAY,component="loadMessage")
	private Block loadMessage() throws MessagingException {
		final LocalMessage lm = getMessage();
		if (lm != null) {
			ms.getMailMessage(lm).setFlag(Flag.SEEN, true);
		} else {
			appErrors.addApplicationError("Please select a message first");
		}
		return resources.getBlock("contentblock");
	}
	
	@OnEvent(value="markasunread")
	private Block markMessageAsUnread() throws MessagingException {
		final LocalMessage lm = getMessage();
		if (lm != null) {
			ms.getMailMessage(lm).setFlag(Flag.SEEN, false);
		} else {
			appErrors.addApplicationError("Please select a message first");
		}
		return resources.getBlock("contentblock");
	}
	
	@OnEvent(value="deletemessage")
	private void deleteMessage() throws MessagingException {
		ms.deleteMailMessage(sso.getStringValue(SESSION_ATTRS.SELECTED_MSG_UID));
		sso.clearValue(SESSION_ATTRS.SELECTED_MSG_UID);
		resources.triggerEvent("reloadContent", new Object[]{}, null);
	}
	
	@OnEvent(value="viewmessage")
	private Block openMessage() {
		return resources.getBlock("messageViewBlock");
	}
	
	@OnEvent(value="reply")
	private Block replyToMessage() throws MessagingException {
		final LocalMessage newMessage = new LocalMessage();
		newMessage.setToAdr(getMessage().getFromAdr());
		return createComposePopup(newMessage);
	}
	
	@OnEvent(value="replyall")
	private Block replyAllToMessage() throws MessagingException {
		final LocalMessage newMessage = new LocalMessage();
		newMessage.setToAdr(getMessage().getFromAdr());
		return createComposePopup(newMessage);
	}

	@OnEvent(value="forward")
	private Block forwardMessage() throws MessagingException {
		final LocalMessage newMessage = new LocalMessage();
		return createComposePopup(newMessage);
	}

	
	private Block createComposePopup(final LocalMessage newMessage) throws MessagingException {
		
		String newUID  =  UUID.randomUUID().toString();
		newMessage.setUID(newUID);
		newMessage.setAccount(as.getAccount().getId());
		newMessage.setFromAdr(as.getAccount().getEmailAddress());
		
		newMessage.setSubject(resources.getMessages().get("reply.prefix") + getMessage().getSubject());
		newMessage.setFolder("DRAFTS");
		
		hsm.getSession().saveOrUpdate(newMessage);
		hsm.commit();
		//trigger compose event, and handle response.
		
		final Holder<Block> holder = new Holder<Block>();
		
		if (resources.triggerEvent("composeMessage", new Object[] {newUID}, new ComponentEventCallback<Block>() {

			public boolean handleResult(Block result) {
				holder.put(result);
				return true;
			}
			
			})) {
			return holder.get();
		} 
		appErrors.addApplicationError("Could not create new message");
		return null;// TODO: check and fix
	}
	
	public boolean getMessageRead() throws MessagingException {
		return ms.getMailMessage(getMessage()).getFlags().contains(Flag.SEEN);
	}
	
	
}
