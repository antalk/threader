package com.paragonict.webapp.threader.entities;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
public class DraftContent implements Serializable {
	
	private static final long serialVersionUID = -4606795730015496488L;
	
	private String UID;
	// base64 encoded content in utf-8!
	private String content; 
	
	@Id
	public String getUID() {
		return UID;
	}

	public void setUID(String uID) {
		UID = uID;
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
