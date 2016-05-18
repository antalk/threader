package com.paragonict.webapp.threader.components;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.OptionModel;
import org.apache.tapestry5.SelectModel;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.RequestParameter;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.internal.OptionModelImpl;
import org.apache.tapestry5.internal.SelectModelImpl;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.ajax.JavaScriptCallback;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.hibernate.criterion.Restrictions;

import com.paragonict.webapp.threader.annotation.RequiresLogin;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.Contact;
import com.paragonict.webapp.threader.entities.DraftContent;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.pages.Index;
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
	
	enum COMPOSEACTION {
		send,
		save;
	}
	
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
	private Form mailEditForm;
	
	@Property
	private	String content;
	
	@Property
	private String recipientAddresses;
	
	private String[] recipients;
	
	private COMPOSEACTION action;
	
	@SetupRender
	public void initializeComposer() {
		mailEditForm.clearErrors();
		// precache and set recipients.
		try {
			if (StringUtils.isNotBlank(getMsg().getToAdr())) {
				recipientAddresses = new InternetAddress(getMsg().getToAdr()).getAddress();
			}
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Cached
	public LocalMessage getMsg() {
		LocalMessage draft = null;
		if (sso.hasValue(SESSION_ATTRS.DRAFT_UID)) {
			draft = (LocalMessage) hsm.getSession().load(LocalMessage.class, sso.getStringValue(SESSION_ATTRS.DRAFT_UID));
			// retrieve content once
			final DraftContent dc = (DraftContent) hsm.getSession().get(DraftContent.class, sso.getStringValue(SESSION_ATTRS.DRAFT_UID));
			if (dc != null) {
				try {
					if (StringUtils.isNotEmpty(dc.getContent())) {
						content = new String(Base64.decodeBase64(dc.getContent()),"UTF-8");
					} else {
						content = "";
					}
				} catch (UnsupportedEncodingException e) {
					// no UTF-8 ? surely you cant be serious
				}
			}

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
		final List<Contact> contacts = hsm.getSession().createCriteria(Contact.class).add(Restrictions.eq("owner", as.getAccount())).list();
		
		final StringBuilder addressBuilder = new StringBuilder();
		
		for (Contact c:contacts) {
			if (c.getName() != null) {
				// personal name
				addressBuilder.append(c.getName()).append(" <").append(c.getMailAddress()).append(">,");
			} else {
				addressBuilder.append(c.getMailAddress()).append(",");
			}
		}
		addressBuilder.append(getMsg().getToAdr()); // last one in list
		final List<OptionModel> addressOptions = new ArrayList<>();
		
		// make the list unique.. 
		Set<InternetAddress> uniqueAddresses = new HashSet<>();
		try {
			for (InternetAddress adr: InternetAddress.parse(addressBuilder.toString())) {
				if (uniqueAddresses.add(adr)) {
					if (adr.getPersonal() != null) {
						addressOptions.add(new OptionModelImpl(adr.getPersonal(), adr.getAddress()));	
					} else {
						addressOptions.add(new OptionModelImpl(adr.getAddress(), adr.getAddress()));
					}	
				}
			}
		} catch (AddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new SelectModelImpl((OptionModel[]) addressOptions.toArray(new OptionModel[] {}));
	}
	
	// send button pressed
	private void onSelectedFromSendButton() {
		action = COMPOSEACTION.send;
	}

	// save button pressed
	private void onSelectedFromSaveButton() {
		action = COMPOSEACTION.save;
	}

	
	@OnEvent(component="mailEditForm",value=EventConstants.VALIDATE)
	private Object validateMessage() {
		if (action.equals(COMPOSEACTION.send)) {
		
			if (StringUtils.isBlank(recipientAddresses)) {
				mailEditForm.recordError("Specify at least one recipient!");
			} else {
				recipients = recipientAddresses.split(";");
				for (String rec:recipients) {
					if (!EmailValidator.getInstance().isValid(rec)) {
						mailEditForm.recordError("Recipient "+rec+" is invalid!");
					}
				}
			}
			if (mailEditForm.getHasErrors()) {
				recipients = null;
				return maileditzone.getBody();
			}
		}
		return null;
	}
	
	@OnEvent(component="mailEditForm",value=EventConstants.SUCCESS)
	private Object handleFormSucces () {
		switch (action) {
		case send :{
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
				// TODO:  send can throw exceptiosn, so this must not be in the succes phase... but BEFORE thst
				
				
				hsm.getSession().delete(hsm.getSession().load(LocalMessage.class,getMsg().getUID()));
				// also delete draft contents if any
				hsm.getSession().delete(hsm.getSession().load(DraftContent.class,getMsg().getUID()));
				
				
				hsm.commit();
				sso.clearValue(SESSION_ATTRS.DRAFT_UID);
			} catch (Exception e) {
				e.printStackTrace();
				mailEditForm.recordError(e.getMessage());
				return maileditzone.getBody();
			}
			arr.addCallback(new JavaScriptCallback() {
				
				@Override
				public void run(JavaScriptSupport javascriptSupport) {
					javascriptSupport.addScript("closeEditModal('maileditzone');");
					
				}
			});
			am.info("Message sent");
			return Index.class;	
		}
		case save :{
			hsm.getSession().persist(getMsg());
			// use GET , can return null
			DraftContent dc = (DraftContent) hsm.getSession().get(DraftContent.class, getMsg().getUID());
			if (dc ==null) {
				dc = new DraftContent();
				dc.setUID(getMsg().getUID());
			}
			if (StringUtils.isNotEmpty(content)) {
				try {
					dc.setContent(Base64.encodeBase64String(content.getBytes("UTF-8")));
				} catch (UnsupportedEncodingException e) {
					// no UTF-8 ? not gonna happen..
				}
			}
			hsm.getSession().saveOrUpdate(dc);
			
			
			hsm.commit();
			
			
			
			return null;
		}
		}
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
