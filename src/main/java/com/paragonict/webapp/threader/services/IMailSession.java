package com.paragonict.webapp.threader.services;

import javax.mail.Session;

public interface IMailSession {

	/**
	 * 
	 * Gets the active {@link Session} for this application
	 * 
	 * @return
	 */
	public Session getSession();
}
