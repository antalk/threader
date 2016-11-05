package com.paragonict.webapp.threader.entities;

import java.util.Date;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;

import org.apache.tapestry5.ioc.services.TypeCoercer;

import com.paragonict.webapp.threader.Utils;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.pop3.POP3Folder;

/*
 * local copy of a pop3/imap message linked by uid (String for POP3 and Long for IMAP) , as String in DB
 * 
 * 
 */
@NamedQueries({
	@NamedQuery(
			hints={@QueryHint(name="org.hibernate.readOnly",value="true")},
			name = LocalMessage.GET_ALL_UIDS,
			query = "select m.UID from LocalMessage m where m.account = :accountid and m.folder = :folder"
	),
	@NamedQuery(
			hints={@QueryHint(name="org.hibernate.readOnly",value="true")},
			name = LocalMessage.COUNT_ALL,
			query = "select count(m.id) from LocalMessage m where m.account = :accountid and m.folder = :folder"
		)
})
@Entity
public class LocalMessage extends AbstractIdEntity {
	
	private static final long serialVersionUID = 6269536651318304698L;

	public static final String GET_ALL_UIDS = "getAllUidsForFolderForUser";
	public static final String COUNT_ALL = "countAllMsgsForFolderForUser";

	enum TYPE {
		MSG,
		DRAFT;
	}
	
	// String for IMAP, Long for POP3 -> Long.asString() 
	// is UID unique enough? should we make a compound key with account? lets find out..
	// i think not, therefor using generated id as key, then uid + folder is unique

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
	private Date receivedDate;
	
	public LocalMessage(final TypeCoercer tc,final Message message,final Long accountId) throws MessagingException {
		if (message.getFolder() instanceof POP3Folder) {
			UID = ((POP3Folder)message.getFolder()).getUID(message);
			
			String dateLines[] = message.getHeader("Date")[0].split("\\r?\\n");
			for (String line:dateLines) {
				try {
					sentDate = tc.coerce(line, Date.class);
					break;// yes
				} catch (RuntimeException e) {
					// booh
				}
			}
			String receivedLines[] = message.getHeader("Received")[0].split("\\r?\\n");
			for (String line:receivedLines) {
				try {
					receivedDate = tc.coerce(line, Date.class);
					break;// yes
				} catch (RuntimeException e) {
					// booh
				}
			}
			System.err.println(" sentDAte" + sentDate);	
			System.err.println(" recDAte" + receivedDate);
			
		} else {
			UID = "" + ((IMAPFolder)message.getFolder()).getUID(message);
			sentDate = message.getSentDate();
			receivedDate = message.getReceivedDate();
		}
		folder = message.getFolder().getFullName();
		fromAdr = Utils.toString(message.getFrom());
		toAdr = Utils.toString(message.getRecipients(RecipientType.TO)[0]);
		subject = message.getSubject();
		account = accountId;
	}
	
	public LocalMessage() {
		// for hibernate, keep in mind the reference to the org. msg is GONE
	}
	
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
	
	public Date getReceivedDate() {
		return receivedDate;
	}

	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
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
