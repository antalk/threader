package com.paragonict.webapp.threader.components;


import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.ComponentEventCallback;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.SelectModel;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.internal.TapestryInternalUtils;
import org.apache.tapestry5.internal.util.Holder;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.paragonict.webapp.threader.entities.Account;
import com.paragonict.webapp.threader.entities.Account.PROTOCOL;
import com.paragonict.webapp.threader.services.IAccountService;

public class AccountEditor {

	@Inject
	private HibernateSessionManager hsm;
	
	@Inject
	private IAccountService as;
	
	@Inject
	private ComponentResources cr;
	
	@Component
	private Form accountedit;
	
	@Property
	private String protocol;
	
	// already loggedin
	@Cached
	public Account getAccount() {
		if (as.isLoggedIn()) {
			protocol = as.getAccount().getProtocol().name();
			return as.getAccount();
		}
		Account newAccount = new Account();
		protocol = PROTOCOL.pop3.name();
		return newAccount;
	}
	
	public boolean getIsLoggedIn() {
		return as.isLoggedIn();
	}
	
	@Cached
	public SelectModel getProtocolModel() {
		List<String> protocols = new ArrayList<String>();
		for (PROTOCOL p: PROTOCOL.values()) {
			protocols.add(p.name());
		}
		return TapestryInternalUtils.toSelectModel(protocols);
	}
	
	
	
	@OnEvent(component="accountedit",value=EventConstants.VALIDATE)
	private void validateExistingAccount() {
		validateAccount();
	}
	
	@OnEvent(component="accountedit",value=EventConstants.SUCCESS)
	private Object updateAccount() {
		if (as.isLoggedIn()) {
			hsm.getSession().saveOrUpdate(getAccount());
		} else {
			hsm.getSession().save(getAccount());
		}
		hsm.commit();
		return throwSuccesEvent();
	}
	
	
	/*
	@OnEvent(component="accountcreate",value=EventConstants.VALIDATE)
	private void validateNewAccount() {
		validateAccount(getNewAccount(), accountcreate);
	}
	
		
		hsm.commit();
		return throwSuccesEvent();
	}
	
	@OnEvent(component="accountcreate",value=EventConstants.SUCCESS)
	private Object createAccount() {
		hsm.getSession().save(getNewAccount());
		hsm.commit();
		return throwSuccesEvent();
	}
	*/
	
	private void validateAccount() {
		if (StringUtils.isBlank(getAccount().getFullName())) {
			accountedit.recordError("Please enter your full name");
		}
		if (StringUtils.isBlank(getAccount().getEmailAddress())) {
			accountedit.recordError("Email address cannot be empty");
		}
		if (StringUtils.isBlank(getAccount().getAccountName())) {
			accountedit.recordError("Account name cannot be empty");
		}
		if (StringUtils.isBlank(getAccount().getHost())) {
			accountedit.recordError("Host cannot be empty");
		}
		if (protocol==null) {
			accountedit.recordError("Protocol cannot be empty");
		}
		if (StringUtils.isBlank(getAccount().getPassword())) {
			accountedit.recordError("Password cannot be empty");
		}
		if (StringUtils.isBlank(getAccount().getSmtpHost())) {
			accountedit.recordError("SMTP Host cannot be empty");
		}
		if (getAccount().getSmtpPort() == null) {
			accountedit.recordError("Specify an smtp port");
		}
	}
	
	private Object throwSuccesEvent() {
		final Holder<Object> retValue = new Holder<Object>();
		
		cr.triggerEvent("accountSuccess", new Object[]{}, new ComponentEventCallback<Object>() {
			
			@Override
			public boolean handleResult(Object result) {
				retValue.put(result);
				return true;
			}
		});
		return retValue.get();
	}
}
