package com.paragonict.webapp.threader.components;

import java.io.IOException;
import java.util.UUID;

import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentEventCallback;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.internal.util.Holder;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.paragonict.tapisser.growl.Message.LEVEL;
import com.paragonict.webapp.threader.annotation.RequiresLogin;
import com.paragonict.webapp.threader.base.BaseComponent;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.DraftContent;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.services.IMailService;

@RequiresLogin
public class Contents extends BaseComponent {

	@SessionState
	private SessionStateObject sso;
	
	@Inject
	private IMailService ms;
	
	@Inject
	private HibernateSessionManager hsm;
		
	@Property
	private String messageContent;
	
	public boolean getMessageSelected() {
		return (sso.hasValue(SESSION_ATTRS.SELECTED_MSG_ID) &&
				sso.hasValue(SESSION_ATTRS.SELECTED_FOLDER));
	}
	
	@Cached
	public LocalMessage getMessage()  throws MessagingException {
		if (getMessageSelected()) {
			// retrieve message
			LocalMessage lm = ms.getLocalMessage(sso.getLongValue(SESSION_ATTRS.SELECTED_MSG_ID));
			if (lm != null) {
					final Message m = ms.getMailMessage(lm);
					if (m==null) {
						lm = null;
						addGrowlerMessage(new com.paragonict.tapisser.growl.Message(LEVEL.ERROR,"Message was not on the server anymore"));
				}
				return lm;
			}
		}
		return null;
	}
	
	@OnEvent(value=EventConstants.PROGRESSIVE_DISPLAY,component="loadMessageContent")
	public void loadMessageContent() throws MessagingException, IOException {
		messageContent = ms.getMessageContent(getMessage());
		
		/*
		
		Whitelist wl = Whitelist.basicWithImages();
		wl.removeAttributes("a", "href");
		Cleaner clean = new Cleaner(wl);
		clean.
		*/
		//return messageContent;
	}
	
	
	@OnEvent(value=EventConstants.PROGRESSIVE_DISPLAY,component="loadMessage")
	private Block loadMessage() throws MessagingException {
		final LocalMessage lm = getMessage();
		if (lm != null) {
			if (!lm.getMessageRead()) {
				ms.getMailMessage(lm).setFlag(Flag.SEEN, true);
				lm.setMessageRead(true);
				hsm.getSession().saveOrUpdate(lm);
				hsm.commit();
			}
			
		} else {
			addGrowlerMessage(new com.paragonict.tapisser.growl.Message(LEVEL.ERROR,"Please select a message first"));
		}
		return getResources().getBlock("contentblock");
	}
	
	@OnEvent(value="markasunread")
	private Block markMessageAsUnread() throws MessagingException {
		final LocalMessage lm = getMessage();
		if (lm != null) {
			if (lm.getMessageRead()) {
				ms.getMailMessage(lm).setFlag(Flag.SEEN, false);
				lm.setMessageRead(false);
				hsm.getSession().saveOrUpdate(lm);
				hsm.commit();
			}
		} else {
			addGrowlerMessage(new com.paragonict.tapisser.growl.Message(LEVEL.ERROR,"Please select a message first"));
		}
		return getResources().getBlock("contentblock");
	}
	
	@OnEvent(value="deletemessage")
	private void deleteMessage() throws MessagingException {
		ms.deleteMailMessage(sso.getLongValue(SESSION_ATTRS.SELECTED_MSG_ID));
		sso.clearValue(SESSION_ATTRS.SELECTED_MSG_ID);
		getResources().triggerEvent("reloadContent", new Object[]{}, null);
	}
	
	@OnEvent(value="viewmessage")
	private Block openMessage() {
		return getResources().getBlock("messageViewBlock");
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
		
		newMessage.setUID(null);
		newMessage.setAccount(getAccountService().getAccount().getId());
		newMessage.setFromAdr(getAccountService().getAccount().getEmailAddress());
		
		newMessage.setSubject(getResources().getMessages().get("reply.prefix") + getMessage().getSubject());
		newMessage.setFolder("DRAFTS");
		
		
		hsm.getSession().saveOrUpdate(newMessage);
		
		// moet je ook niet de content meenemen >???
				DraftContent dc = new DraftContent();
				dc.setLocalMessage(newMessage);
				try {
					dc.setContent(ms.getMessageContent(getMessage()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				hsm.getSession().persist(dc);
		
		
		hsm.commit();
		//trigger compose event, and handle response.
		
		final Holder<Block> holder = new Holder<Block>();
		
		if (getResources().triggerEvent("composeMessage", new Object[] {newMessage.getId()}, new ComponentEventCallback<Block>() {

			public boolean handleResult(Block result) {
				holder.put(result);
				return true;
			}
			
			})) {
			return holder.get();
		} 
		addGrowlerMessage(new com.paragonict.tapisser.growl.Message(LEVEL.ERROR,"Could not create new message"));
		return null;// TODO: check and fix
	}
}
