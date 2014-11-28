package com.paragonict.webapp.threader.services;

import java.io.IOException;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;

import org.apache.tapestry5.grid.SortConstraint;

import com.paragonict.webapp.threader.beans.ClientMessage;
import com.paragonict.webapp.threader.entities.Folder;

public interface IMailService {

	// get all folder and return root
	public Folder getFolders(boolean renew) throws MessagingException;
	
	public List<ClientMessage> getMessages(final String folder,int start,int end,SortConstraint sc) throws MessagingException;
	
	public Integer getNrOfMessages(final String folder) throws MessagingException;
	
	/**
	 * Gets the complete message, incl. content as a client side object and provides caching
	 * 
	 * @param folder
	 * @param msgId
	 * @return
	 * @throws MessagingException
	 */
	public ClientMessage getMessage(final String folder, final Integer msgId) throws MessagingException;
	
	//public String getMessageContent(final Part p) throws IOException, MessagingException;
}
