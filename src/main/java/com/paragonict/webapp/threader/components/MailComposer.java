package com.paragonict.webapp.threader.components;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.SelectModel;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.corelib.components.BeanEditForm;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.internal.TapestryInternalUtils;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.ajax.JavaScriptCallback;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.hibernate.Session;

import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailService;
import com.paragonict.webapp.threader.services.IMailSession;
import com.sun.mail.smtp.SMTPMessage;

/**
 * Create or edit/forward/reply a mailmessage
 * 
 * @author avankalleveen
 *
 */
@Import(library="MailComposer.js")
public class MailComposer {
	
	@Inject
	private IAccountService as;
	
	@Inject
	private IMailSession ss;
	
	@Inject
	private IMailService ms;
	
	@Inject
	private AlertManager am;
	
	@Inject
	private AjaxResponseRenderer arr;
	
	@Inject
	private Session session;
	
	@Inject
	private ComponentResources resources;
	
	@Component
	private Zone maileditzone;
	
	@Component
	private BeanEditForm mailEditor;
	
	@Property
	private	String content;
	
	@SetupRender
	public void initializeComposer() {
		content = "";
		try {
			if (getMsg().getFolder() != null && getMsg().getMsgid() != null) {
				content = ms.getMessageContent(ms.getMessage(getMsg().getFolder(), getMsg().getMsgid()));
				content = getMsg().getFromAdr() + " wrote:\n\r<blockquote>" + content + "</blockquote>";
			}
		} catch (IOException | MessagingException e) {
			e.printStackTrace();
		}
		mailEditor.clearErrors();
	}
	
	@Cached
	public com.paragonict.webapp.threader.entities.Message getMsg() {
		return (com.paragonict.webapp.threader.entities.Message) session.load(com.paragonict.webapp.threader.entities.Message.class, as.getAccount().getId());
	}
	
	public SelectModel getAddressBook() {
		// TODO: get entries from address book / or recently used addresses... hmm
		// for now some hardcoded addresses
		final List<String> adressBook = new LinkedList<String>();
		
		adressBook.add("test@localhost.nl");
		adressBook.add("dev.intercommit@gmail.com");
		
		return TapestryInternalUtils.toSelectModel(adressBook);
	}
	
	
	@OnEvent(component="mailEditor",value=EventConstants.VALIDATE)
	private Object validateMessage() {
		if (StringUtils.isBlank(getMsg().getToAdr())) {
			mailEditor.recordError("Specify at least one recipient!");
		}
		if (mailEditor.getHasErrors()) {
			return maileditzone.getBody();
		}
		return null;
	}
	
	@OnEvent(component="mailEditor",value=EventConstants.SUCCESS)
	private Object sendMessage() {
		SMTPMessage sm = new SMTPMessage(ss.getSMTPSession());
		try {
			sm.addFrom(new InternetAddress[] { new InternetAddress(getMsg().getFromAdr())});
			sm.addRecipient(RecipientType.TO, new InternetAddress(getMsg().getToAdr()));
			sm.setSubject(getMsg().getSubject());
			if (StringUtils.isBlank(content)) {
				content = "";
			} 
			sm.setContent(content, "text/plain");
			Transport.send(sm);
			//TODO: delete persistent msg
			
			
		} catch (Exception e) {
			e.printStackTrace();
			mailEditor.recordError(e.getMessage());
			return maileditzone.getBody();
		}
		arr.addCallback(new JavaScriptCallback() {
			
			@Override
			public void run(JavaScriptSupport javascriptSupport) {
				javascriptSupport.addScript("closeEditModal('maileditzone');");
				
			}
		});
		am.info("Message sent");
		return null;
	}
}
