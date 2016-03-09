package com.paragonict.webapp.threader.services;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * per thread object to read write from a {@link Store}
 * 
 * 
 * @author Antal
 *
 */

public interface IMailStore {
		
	public IMailStore getStore(final Session session) throws MessagingException;
	
	public Folder getDefaultFolder() throws MessagingException;

	public Folder getFolder(String name) throws MessagingException;
	
	// registers a folder in this session egible to close and the end of the session.
	public void registerFolder(Folder folder);

}
		
