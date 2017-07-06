package com.paragonict.webapp.threader.services.impl;


import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.internal.util.TapestryException;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.hibernate.Session;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.Account;
import com.paragonict.webapp.threader.services.IAccountService;

public class AccountServiceImpl implements IAccountService {

	@Inject
	private Session session;
	
	private Long accountId;
	
	private Account account;// initially null only filled when asked for.
	
	@PostInjection
	public void init(ApplicationStateManager asm) {
		accountId = null;
		if (asm.exists(SessionStateObject.class)) {
			final SessionStateObject sso = asm.get(SessionStateObject.class);
			if (sso.hasValue(SESSION_ATTRS.USER_ID)) {
				Long id = sso.getLongValue(SESSION_ATTRS.USER_ID);
				// throw an exception if the account does not exist ?!?wtf happendthen?
				// hmm load or get?
				accountId = (Long) session.getNamedQuery(Account.GET_ACCOUNTID).setParameter("accountid", id).uniqueResult();
			}
		}
	}
	
	public boolean isLoggedIn() {
		return accountId != null;
	}

	public Long getAccountID() {
		return accountId;
	}
	
	public Account getAccount() {
		if (account == null) {
			account = (Account) session.load(Account.class, getAccountID()); // loads all account details.
			if (account == null) {
				// still null? then no cigar !
				throw new TapestryException("Account is null", new NullPointerException());
			}
		}
		return account;
	}

}
