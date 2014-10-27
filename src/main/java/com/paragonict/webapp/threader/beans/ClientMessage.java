package com.paragonict.webapp.threader.beans;

import java.util.Date;

import javax.mail.Message;

/**
 * a disconnect/ stripped version of {@link Message} to improve performance. 
 * 
 * @author avankalleveen
 *
 */
public class ClientMessage {

	private Integer msgId;
	private String from;
	private String subject;
	private Date sentDate;
	private boolean read;
	
	public Integer getMsgId() {
		return msgId;
	}
	public void setMsgId(Integer msgId) {
		this.msgId = msgId;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public Date getSentDate() {
		return sentDate;
	}
	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}
	public boolean isRead() {
		return read;
	}
	public void setRead(boolean read) {
		this.read = read;
	}
	
	
}
