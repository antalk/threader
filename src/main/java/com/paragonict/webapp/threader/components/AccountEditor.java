package com.paragonict.webapp.threader.components;


import org.apache.commons.lang3.StringUtils;
import org.apache.tapestry5.ComponentEventCallback;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.corelib.components.BeanEditForm;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.internal.util.Holder;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.paragonict.webapp.threader.entities.Account;
import com.paragonict.webapp.threader.services.IAccountService;

public class AccountEditor {

	@Inject
	private HibernateSessionManager hsm;
	
	@Inject
	private IAccountService as;
	
	@Inject
	private ComponentResources cr;
	
	@Component
	private BeanEditForm accountcreate;

	@Component
	private BeanEditForm accountedit;

	
	// already loggedin
	@Cached
	public Account getAccount() {
		return as.getAccount();
	}
	
	@Cached
	public Account getNewAccount() {
		Account account = new Account();
		return account;
	}
	
	public boolean getIsLoggedIn() {
		return as.isLoggedIn();
	}
	
	@OnEvent(component="accountedit",value=EventConstants.VALIDATE)
	private void validateExistingAccount() {
		validateAccount(getAccount(), accountedit);
	}
	
	
	@OnEvent(component="accountcreate",value=EventConstants.VALIDATE)
	private void validateNewAccount() {
		validateAccount(getNewAccount(), accountcreate);
	}
	
	@OnEvent(component="accountedit",value=EventConstants.SUCCESS)
	private Object updateAccount() {
		hsm.getSession().saveOrUpdate(getAccount());
		hsm.commit();
		return throwSuccesEvent();
	}
	
	@OnEvent(component="accountcreate",value=EventConstants.SUCCESS)
	private Object createAccount() {
		hsm.getSession().save(getNewAccount());
		hsm.commit();
		return throwSuccesEvent();
	}
	
	
	private void validateAccount(final Account acc,final BeanEditForm form) {
		if (StringUtils.isBlank(acc.getName())) {
			form.recordError("Name cannot be empty");
		}
		if (StringUtils.isBlank(acc.getEmailAddress())) {
			form.recordError("Email address cannot be empty");
		}
		if (StringUtils.isBlank(acc.getHost())) {
			form.recordError("Host cannot be empty");
		}
		if (StringUtils.isBlank(acc.getPassword())) {
			form.recordError("Password cannot be empty");
		}
		if (acc.getProtocol()==null) {
			form.recordError("Protocol cannot be empty");
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
