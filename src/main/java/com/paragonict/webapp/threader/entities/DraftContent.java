package com.paragonict.webapp.threader.entities;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

@NamedQueries({
	@NamedQuery(
			name = DraftContent.GETDRAFTFORMSG,
			query = "select dc from DraftContent dc where dc.localMessage = :localMessage"
	)
})
@Entity
public class DraftContent extends AbstractIdEntity {
	
	private static final long serialVersionUID = -4606795730015496488L;
	
	public static final String GETDRAFTFORMSG = "GetDraftContentForMessage";
	
	// base64 encoded content in utf-8!
	private String content; 
	
	private LocalMessage localMessage;
	
	@OneToOne(optional=true)
	public LocalMessage getLocalMessage() {
		return localMessage;
	}
	
	public void setLocalMessage(LocalMessage localMessage) {
		this.localMessage = localMessage;
	}
	
	/**
	 * base64 UTF-8 encoded field
	 * 
	 * TODO: we can even zip it...
	 * 
	 * @return
	 */
	@Lob
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}

}
