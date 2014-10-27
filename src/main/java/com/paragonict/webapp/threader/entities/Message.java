package com.paragonict.webapp.threader.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

/*
 * A (not yet) persistent Message the current user is working on, e.g. new, reply, forward. etc.
 * Just to have some storage object not in memory, but in database.
 * 
 */
@Entity
public class Message {

	private static final long serialVersionUID = 6269536651318304698L;

	// for whom? 
	private Long account;
	
	// keep a reference to any existing mail?
	private String folder;
	private Integer msgid;
	
	// the fields to edit
	private String fromAdr;
	private String toAdr;
	private String ccAdr;
	private String bccAdr;
	private String subject;
	
	// can only have 1 edited message at a time per account!
	@Id
	public Long getAccount() {
		return account;
	}

	public void setAccount(Long account) {
		this.account = account;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public Integer getMsgid() {
		return msgid;
	}

	public void setMsgid(Integer msgid) {
		this.msgid = msgid;
	}

	
	public String getFromAdr() {
		return fromAdr;
	}

	public void setFromAdr(String fromAdr) {
		this.fromAdr = fromAdr;
	}

	public String getToAdr() {
		return toAdr;
	}

	public void setToAdr(String toAdr) {
		this.toAdr = toAdr;
	}

	public String getCcAdr() {
		return ccAdr;
	}

	public void setCcAdr(String ccAdr) {
		this.ccAdr = ccAdr;
	}

	public String getBccAdr() {
		return bccAdr;
	}

	public void setBccAdr(String bccAdr) {
		this.bccAdr = bccAdr;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
}
