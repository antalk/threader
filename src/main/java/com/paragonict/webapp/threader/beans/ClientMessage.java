package com.paragonict.webapp.threader.beans;

import java.io.Serializable;
import java.util.Date;

import javax.mail.Message;

/**
 * a disconnect/ stripped version of {@link Message} to improve performance. 
 * 
 * @author avankalleveen
 *
 */
public class ClientMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3317865513040961960L;
	
	private Integer msgId;
	private String from;
	private String subject;
	private Date sentDate;
	private boolean read;
	private String content;
	private String contentType;
	
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
	
	
}
