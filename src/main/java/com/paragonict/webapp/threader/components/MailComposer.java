package com.paragonict.webapp.threader.components;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.PersistenceConstants;
import org.apache.tapestry5.SelectModel;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.RequestParameter;
import org.apache.tapestry5.annotations.SessionState;
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
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.Contact;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailService;
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
	private IMailService ms;
	
	@Inject
	private AlertManager am;
	
	@Inject
	private AjaxResponseRenderer arr;
	
	@Inject
	private HibernateSessionManager hsm;
	
	@Inject
	private ComponentResources resources;
	
	@SessionState
	private SessionStateObject sso;
	
	
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
		/** /try {
			if (getMsg().getFolder() != null && getMsg().getUID() != null) {
				//ms.getMessage(getMsg().getFolder(), getMsg().getMsgid()).getContent();
				// TODO: content = ms.getMessage();
				content = getMsg().getFromAdr() + " wrote:\n\r<blockquote>" + content + "</blockquote>";
			}
		//} catch (MessagingException e) {
			//e.printStackTrace();
		//}**/
		mailEditor.clearErrors();
	}
	
	@Cached
	public LocalMessage getMsg() {
		LocalMessage draft = null;
		if (sso.hasValue(SESSION_ATTRS.DRAFT_UID)) {
			draft = (LocalMessage) hsm.getSession().load(LocalMessage.class, sso.getStringValue(SESSION_ATTRS.DRAFT_UID));
		}
		if (draft == null) {
			sso.putValue(SESSION_ATTRS.DRAFT_UID, UUID.randomUUID().toString());
			
			draft = new LocalMessage();
			draft.setAccount(as.getAccount().getId());
			draft.setFromAdr(as.getAccount().getEmailAddress());
			draft.setFolder("DRAFTS");
			draft.setUID(sso.getStringValue(SESSION_ATTRS.DRAFT_UID));
			hsm.getSession().persist(draft);
			hsm.commit();
		}
		return draft;
	}
	
	@Cached
	public SelectModel getAddressBookModel() {
		final List<String> adressBook = new LinkedList<String>();
		
		List<Contact> contacts = hsm.getSession().createCriteria(Contact.class).add(Restrictions.eq("owner", as.getAccount())).list();
		
		for (Contact c:contacts) {
			adressBook.add(c.getMailAddress());
		}
		if (!adressBook.contains(getMsg().getToAdr())) {
			adressBook.add(getMsg().getToAdr());
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
		final SMTPMessage sm = ms.createMessage();
		try {
			sm.addFrom(new InternetAddress[] { new InternetAddress(getMsg().getFromAdr())});
			for (String rec : recipients) {
				sm.addRecipient(RecipientType.TO, new InternetAddress(rec));	
			}
			sm.setSentDate(new Date());
			sm.setSubject(getMsg().getSubject());
			if (StringUtils.isBlank(content)) {
				content = "";
			} 
			sm.setContent(content, "text/plain");
			Transport.send(sm);
			//TODO: delete persistent msg
			
			sso.clearValue(SESSION_ATTRS.DRAFT_UID);
			
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
