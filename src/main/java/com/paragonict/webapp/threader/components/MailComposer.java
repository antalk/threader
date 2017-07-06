package com.paragonict.webapp.threader.components;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage.RecipientType;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.OptionModel;
import org.apache.tapestry5.SelectModel;
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
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import com.paragonict.tapisser.growl.Message;
import com.paragonict.webapp.threader.annotation.RequiresLogin;
import com.paragonict.webapp.threader.base.BaseComponent;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.Contact;
import com.paragonict.webapp.threader.entities.DraftContent;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.pages.Index;
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
public class MailComposer extends BaseComponent {
	
	enum COMPOSEACTION {
		send,
		save;
	}
	
	@Inject
	private IMailService ms;
	
	@Inject
	private AjaxResponseRenderer arr;
	
	@Inject
	private HibernateSessionManager hsm;
	
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
		if (sso.hasValue(SESSION_ATTRS.DRAFT_ID)) {
			draft = (LocalMessage) hsm.getSession().load(LocalMessage.class, sso.getLongValue(SESSION_ATTRS.DRAFT_ID));
			// retrieve content once
			final DraftContent dc = (DraftContent) hsm.getSession().getNamedQuery(DraftContent.GETDRAFTFORMSG).setEntity("localMessage", draft).uniqueResult();
			if (dc != null) {
				try {
					if (StringUtils.isNotEmpty(dc.getContent())) {
						content = new String(Base64.decodeBase64(dc.getContent()),CharEncoding.UTF_8);
					} else {
						content = "";
					}
				} catch (UnsupportedEncodingException e) {
					// no UTF-8 ? surely you cant be serious
				}
			}

		}
		if (draft == null) {
			
			draft = new LocalMessage();
			draft.setAccount(getAccountService().getAccountID());
			draft.setFromAdr(getAccountService().getAccount().getEmailAddress());
			draft.setFolder("DRAFTS");
			hsm.getSession().persist(draft);
			hsm.commit();
			sso.putValue(SESSION_ATTRS.DRAFT_ID, draft.getId());
			
		}
		
		return draft;
	}
	
	@Cached
	public SelectModel getAddressBookModel() {
		final List<Contact> contacts = hsm.getSession().createCriteria(Contact.class).add(Restrictions.eq("owner", getAccountService().getAccount())).list();
		
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
	private Object prepareMessage() {
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
			} catch (Exception e) {
				e.printStackTrace();
				// get to the root cause
				
				
				
				mailEditForm.recordError(ExceptionUtils.getRootCause(e).getMessage());
				return maileditzone.getBody();
			}
			
		}
		return null; // continue to SUCCESS event
	}
	
	@OnEvent(component="mailEditForm",value=EventConstants.SUCCESS)
	private Object handleFormSucces () {
		switch (action) {
		case send :{
			// first delete draft
			final DraftContent df = (DraftContent) hsm.getSession().getNamedQuery(DraftContent.GETDRAFTFORMSG).setEntity("localMessage", getMsg()).uniqueResult();
			if (df != null) {
				hsm.getSession().delete(df);
				// becasue of Casade. the localmessage will alsobe removed?
			}
			// also delete message
			hsm.getSession().delete(hsm.getSession().load(LocalMessage.class,getMsg().getId()));
			
			hsm.commit();
			sso.clearValue(SESSION_ATTRS.DRAFT_ID);
			
			arr.addCallback(new JavaScriptCallback() {
				
				@Override
				public void run(JavaScriptSupport javascriptSupport) {
					javascriptSupport.addScript("closeEditModal('maileditzone');");
					
				}
			});
			addGrowlerMessage(new Message("Message sent"));
			return Index.class;	
		}
		case save :{
			hsm.getSession().saveOrUpdate(getMsg());
			// use GET , can return null
			final Criteria crit = hsm.getSession().createCriteria(DraftContent.class);
			crit.add(Restrictions.eq("localMessage", getMsg()));
			DraftContent dc = (DraftContent) crit.uniqueResult();
			if (dc ==null) {
				dc = new DraftContent();
				dc.setLocalMessage(getMsg());
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
			addGrowlerMessage(new Message("Draft saved"));
		}
		}
		return maileditzone;
	}
	
	
	@OnEvent(value="chosen_update")
	private void addToAddressBook(@RequestParameter("addedvalue") String address) {
		
		if (EmailValidator.getInstance().isValid(address)) {
			Contact newContact = (Contact) hsm.getSession().createCriteria(Contact.class).add(Restrictions.eq("mailAddress", address)).add(Restrictions.eq("owner", getAccountService().getAccount())).uniqueResult();
			if (newContact == null) {
				newContact = new Contact();
				newContact.setOwner(getAccountService().getAccount());
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
