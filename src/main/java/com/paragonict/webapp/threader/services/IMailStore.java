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

	//TODO: make clear what the difference is in register and get folder...?!?!
	public Folder getFolder(String name) throws MessagingException;
	
	// registers a folder in this session legible to close and the end of the session.
	public void registerFolder(Folder folder);
	
	/**
	 * Gets an unmanaged store, meaning it will NOT be managed by this service.
	 * The end-user is responsible for closing it, and its folders; to prevent leaking of resources
	 * 
	 * @return
	 */
	public Store getUnmanagedStore() throws MessagingException;
}
		
