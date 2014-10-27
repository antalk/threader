package com.paragonict.webapp.threader.services.impl;


import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.hibernate.Session;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.Account;
import com.paragonict.webapp.threader.services.IAccountService;

public class AccountServiceImpl implements IAccountService {

	@Inject
	private Session session;
	
	private Account account;
	
	@PostInjection
	public void init(ApplicationStateManager asm) {
		account = null;
		if (asm.exists(SessionStateObject.class)) {
			final SessionStateObject sso = asm.get(SessionStateObject.class);
			Long id = (Long) sso.getValue(SESSION_ATTRS.USER_ID);
			if (id != null) {
				account = (Account) session.get(Account.class, id);
			}
		}
	}
	
	public boolean isLoggedIn() {
		return account != null;
	}

	public Account getAccount() {
		return account;
	}

}
