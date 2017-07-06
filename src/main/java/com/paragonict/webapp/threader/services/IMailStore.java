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
	/**
	 * Gets a 'cached' {@link Folder} from the store.
	 * 
	 * @param name
	 * @return
	 * @throws MessagingException
	 */
	public Folder getFolder(String name) throws MessagingException;
	
	// opens a folder for RW and regsiters this folder to this session legible to close and the end of the session.
	public void registerFolder(final Folder folder) throws MessagingException;
	
	/**
	 * Gets an unmanaged store, meaning it will NOT be managed by this service.
	 * The end-user is responsible for closing it, and its folders; to prevent leaking of resources
	 * 
	 * @return {@link Store}
	 */
	public Store getUnmanagedStore() throws MessagingException;
}
		
