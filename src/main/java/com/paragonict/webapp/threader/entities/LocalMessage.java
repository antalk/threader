package com.paragonict.webapp.threader.entities;

import java.io.Serializable;
import java.util.Date;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.paragonict.webapp.threader.Utils;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.pop3.POP3Folder;

/*
 * local copy of a pop3/imap message linked by uid (String for POP3 and Long for IMAP) , as String in DB
 * 
 * 
 */
@Entity
public class LocalMessage implements Serializable {

	private static final long serialVersionUID = 6269536651318304698L;

	enum TYPE {
		MSG,
		DRAFT;
	}
	
	// String for IMAP, Long for POP3 -> Long.asString() 
	// is UID unique enough? should we make a compound key with account? lets find out..
	private String UID;
	
	// for whom? 
	private Long account;
	private String folder;
	
	// the fields to edit
	// i've added Adr , as 'from' is a reserved DB keyword
	private String fromAdr;
	private String toAdr;
	private String ccAdr;
	private String bccAdr;
	private String subject;
	
	private Date sentDate;
	//
	
	public LocalMessage(final Message message,final Account theAccount) throws MessagingException {
		if (message.getFolder() instanceof POP3Folder) {
			UID = ((POP3Folder)message.getFolder()).getUID(message);
		} else {
			UID = "" + ((IMAPFolder)message.getFolder()).getUID(message);
		}
		folder = message.getFolder().getName();
		fromAdr = Utils.toString(message.getFrom());
		toAdr = Utils.toString(message.getRecipients(RecipientType.TO)[0]);
		sentDate = message.getSentDate();
		subject = message.getSubject();
		account = theAccount.getId();
	}
	
	public LocalMessage() {
		// for hibernat, keep in mind the reference to the org. msg is GONE
	}
	
	@Id
	public String getUID() {
		return UID;
	}

	public void setUID(String uID) {
		UID = uID;
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

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

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

	

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
}
