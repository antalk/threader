package com.paragonict.webapp.threader.services;

import java.io.IOException;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.tapestry5.grid.SortConstraint;

import com.paragonict.webapp.threader.entities.Folder;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.sun.mail.smtp.SMTPMessage;

public interface IMailService {

	// get all folder and return root
	public Folder getFolders(boolean renew) throws MessagingException;
	
	public List<LocalMessage> getMessages(final String folder,int start,int end,int mailTotal,SortConstraint sc) throws MessagingException;
	
	public Integer getNrOfMessages(final String folder) throws MessagingException;
	
	public LocalMessage getLocalMessage(final Long id) throws MessagingException;
	
	/**
	 * Get the (cached) {@link Message} for {@link LocalMessage}
	 * 
	 * @param message
	 * @return null if the message was not on the mailserver anymore !
	 * 
	 */
	public Message getMailMessage(final LocalMessage message) throws MessagingException;
	
	public SMTPMessage createMessage();
	
	public String getMessageContent(final LocalMessage message) throws IOException,MessagingException;
	
	public boolean isMessageRead(final LocalMessage message) throws MessagingException;
	
	/**
	 * Tries to delete the mail message on the mail server ( if it exists)
	 * also removes the messages from the local db, if exist
	 * And updates the localcache.
	 * 
	 * @param id's of the messages.
	 * @return true or false
	 */
	public boolean deleteMailMessage(final Long... ids) throws MessagingException;
	
	

}
