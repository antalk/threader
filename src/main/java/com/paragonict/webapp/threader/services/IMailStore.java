package com.paragonict.webapp.threader.services;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;

/**
 * per thread object to read write from a {@link Store}
 * 
 * 
 * @author Antal
 *
 */

public interface IMailStore {
		
	public Folder getDefaultFolder() throws MessagingException;

	public Folder getFolder(String name) throws MessagingException;
	
	// registers a folder in this session legible to close and the end of the session.
	public void registerFolder(Folder folder);
	
	// premature close the store, for example within an ajax request, when the request has not ended, but needs to end..
	//public void closeStore();

}
		
