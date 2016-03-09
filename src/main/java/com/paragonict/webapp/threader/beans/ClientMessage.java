package com.paragonict.webapp.threader.beans;

import java.io.Serializable;

import javax.mail.Message;

/**
 * a disconnect/ stripped version of {@link Message} to improve performance. 
 * 
 * @author avankalleveen
 *
 */
@Deprecated
public class ClientMessage implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3317865513040961960L;
	
	/*
	private final String UUID;
	private final String from;
	private final String subject;
	private final Date sentDate;
	
	// setable
	private  boolean read;
	private String content;
	private String contentType;
	
	public ClientMessage(final Message msg,final String UUID) throws MessagingException {
		this.UUID = UUID;
		this.from = Utils.addressesToString(msg.getFrom());
		this.subject = msg.getSubject();
		this.sentDate = msg.getSentDate();
		this.read = false;
	}
	
	public String getUUID() {
		return UUID;
	}
	
	public String getFrom() {
		return from;
	}
	
	public String getSubject() {
		return subject;
	}
	
	public Date getSentDate() {
		return sentDate;
	}
	
	public boolean isRead() {
		return read;
	}
	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	*/
	
}
