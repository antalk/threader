package com.paragonict.webapp.threader.services;

import java.io.IOException;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;

import org.apache.tapestry5.grid.SortConstraint;

import com.paragonict.webapp.threader.entities.Folder;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.sun.mail.smtp.SMTPMessage;

public interface IMailService {

	// get all folder and return root
	public Folder getFolders(boolean renew) throws MessagingException;
	
	public List<LocalMessage> getMessages(final String folder,int start,int end,int mailTotal,SortConstraint sc) throws MessagingException;
	
	public Integer getNrOfMessages(final String folder) throws MessagingException;
	
	public LocalMessage getLocalMessage(final String UID) throws MessagingException;
	
	/**
	 * Get the (cached) {@link Message} for {@link LocalMessage}
	 * 
	 * @param message
	 * @return
	 * @throws MessagingException if the {@link Message} does not exist anymore.
	 */
	public Message getMailMessage(final LocalMessage message) throws MessagingException;
		
	public SMTPMessage createMessage();
	
	public String getMessageContent(Part p) throws IOException,MessagingException;
	
	public boolean isMessageRead(final LocalMessage message) throws MessagingException;
}
