package com.paragonict.webapp.threader.mail;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.URLName;

import com.paragonict.webapp.threader.services.IMailSession;


/**
 * Some 'wrapper' around a real {@link Store} to keep hold of open folders and close these at the end of a request.
 * 
 * TODO: move this INTO {@link IMailSession}
 * 
 * @author avankalleveen
 *
 */
public class MailStore {

	private final Store _delegate;
	private List<Folder> requestedFolders;
	
	public MailStore(final Store store) {
		this._delegate = store;
		requestedFolders = new ArrayList<Folder>(5); // start with 5..
	}

	public Folder getDefaultFolder() throws MessagingException {
		requestedFolders.add(_delegate.getDefaultFolder());		
		return requestedFolders.get(requestedFolders.size()-1); // return the latest..
	}

	public Folder getFolder(String name) throws MessagingException {
		requestedFolders.add(_delegate.getFolder(name));
		return requestedFolders.get(requestedFolders.size()-1); // return the latest..
	}

	public Folder getFolder(URLName url) throws MessagingException {
		requestedFolders.add(_delegate.getFolder(url));		
		return requestedFolders.get(requestedFolders.size()-1); // return the latest..
	}
	
	public Folder[] getPersonalNamespaces() throws MessagingException {
		return _delegate.getPersonalNamespaces();
	}
	
	public Folder[] getSharedNamespaces() throws MessagingException {
		return _delegate.getSharedNamespaces();
	}
	
	public Folder[] getUserNamespaces(String user) throws MessagingException {
		return _delegate.getUserNamespaces(user);
	}
	
	
	public void closeMailStore() {
		try {
			for (Folder f: requestedFolders) {
				if (f.isOpen()) {
					System.out.println("EOR Closing folder :" + f);
					if (f.getDeletedMessageCount() > 0) {
						f.close(true);
					} else {
						f.close(false);
					}
				}
			}
			if (_delegate.isConnected()) {
				System.out.println("EOR Closing session :");
				_delegate.close();
			}
		} catch (MessagingException me) {
			System.err.println("Error closing mailStore: " + me.getMessage());
		}
	}
	
}
