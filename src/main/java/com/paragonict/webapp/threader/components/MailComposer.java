package com.paragonict.webapp.threader.components;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.SelectModel;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.RequestParameter;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.corelib.components.BeanEditForm;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.internal.TapestryInternalUtils;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.ajax.JavaScriptCallback;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.hibernate.criterion.Restrictions;

import com.paragonict.webapp.threader.annotation.RequiresLogin;
import com.paragonict.webapp.threader.entities.Contact;
import com.paragonict.webapp.threader.entities.Message;
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
@RequiresLogin
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
	private HibernateSessionManager hsm;
	
	@Inject
	private ComponentResources resources;
	
	@Component
	private Zone maileditzone;
	
	@Component
	private BeanEditForm mailEditor;
	
	@Property
	private	String content;
	
	private String[] recipients;
	
	@SetupRender
	public void initializeComposer() {
		content = "";
		try {
			if (getMsg().getFolder() != null && getMsg().getMsgid() != null) {
				ms.getMessage(getMsg().getFolder(), getMsg().getMsgid()).getContent();
				// TODO: content = ms.getMessage();
				content = getMsg().getFromAdr() + " wrote:\n\r<blockquote>" + content + "</blockquote>";
			}
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		mailEditor.clearErrors();
	}
	
	@Cached
	public com.paragonict.webapp.threader.entities.Message getMsg() {
		Message newMessage = (Message) hsm.getSession().get(Message.class, as.getAccount().getId());
		if (newMessage == null) {
			newMessage = new Message();
			newMessage.setAccount(as.getAccount().getId());
		}
		try {
			newMessage.setFromAdr(new InternetAddress(as.getAccount().getEmailAddress(),as.getAccount().getFullName()).toString());
		} catch (UnsupportedEncodingException e) {
			// hmm.. will not happen?
			e.printStackTrace();
		}
		hsm.getSession().saveOrUpdate(newMessage);
		hsm.commit();
		return newMessage;
	}
	
	@Cached
	public SelectModel getAddressBook() {
		final List<String> adressBook = new LinkedList<String>();
		
		List<Contact> contacts = hsm.getSession().createCriteria(Contact.class).add(Restrictions.eq("owner", as.getAccount())).list();
		
		for (Contact c:contacts) {
			adressBook.add(c.getMailAddress());
		}
		return TapestryInternalUtils.toSelectModel(adressBook);
	}
	
	
	@OnEvent(component="mailEditor",value=EventConstants.VALIDATE)
	private Object validateMessage() {
		if (StringUtils.isBlank(getMsg().getToAdr())) {
			mailEditor.recordError("Specify at least one recipient!");
		} else {
			recipients = getMsg().getToAdr().split("\\s");
			for (String rec:recipients) {
				if (!EmailValidator.getInstance().isValid(rec)) {
					mailEditor.recordError("Recipient ["+rec+"] is invalid!");
				}
			}
		}
		if (mailEditor.getHasErrors()) {
			recipients = null;
			return maileditzone.getBody();
		}
		
		return null;
	}
	
	@OnEvent(component="mailEditor",value=EventConstants.SUCCESS)
	private Object sendMessage() {
		SMTPMessage sm = new SMTPMessage(ss.getSession());
		try {
			sm.addFrom(new InternetAddress[] { new InternetAddress(getMsg().getFromAdr())});
			for (String rec : recipients) {
				sm.addRecipient(RecipientType.TO, new InternetAddress(rec));	
			}
			
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
	
	
	@OnEvent(value="chosen_update")
	private void addToAddressBook(@RequestParameter("addedvalue") String address) {
		
		if (EmailValidator.getInstance().isValid(address)) {
			Contact newContact = (Contact) hsm.getSession().createCriteria(Contact.class).add(Restrictions.eq("mailAddress", address)).add(Restrictions.eq("owner", as.getAccount())).uniqueResult();
			if (newContact == null) {
				newContact = new Contact();
				newContact.setOwner(as.getAccount());
				newContact.setMailAddress(address);
				hsm.getSession().save(newContact);
				hsm.commit();
				System.err.println("added new contact");
			}
		} else {
			System.err.println("invalid address, not adding to contacts");
		}
				
	}
}
