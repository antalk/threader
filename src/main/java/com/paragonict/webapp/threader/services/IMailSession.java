package com.paragonict.webapp.threader.services;

import javax.mail.Session;

public interface IMailSession {

	/**
	 * 
	 * Gets the active {@link Session} for this account
	 * 
	 * @return
	 */
	public Session getSession(final IAccountService as);
	
	public void clearSession(final IAccountService as);
}
