package com.paragonict.webapp.threader.entities;

import javax.persistence.Entity;
import javax.persistence.OneToOne;



@Entity
public class Folder extends AbstractIdEntity {

	private static final long serialVersionUID = 782268464131746352L;
	
	private Folder parent;
	private Account account;
	private String name;
	private String label;
	private Integer unreadMsgs;
	private Boolean hasChilds;
	
	//imap specific fields
	private Long uidValidity;
	
	
	@OneToOne(optional=true)
	public Folder getParent() {
		return parent;
	}

	public void setParent(Folder parent) {
		this.parent = parent;
	}

	@OneToOne
	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Integer getUnreadMsgs() {
		if (unreadMsgs == null) {
			unreadMsgs = -1;
		}
		return unreadMsgs;
	}

	public void setUnreadMsgs(Integer unreadMsgs) {
		this.unreadMsgs = unreadMsgs;
	}

	public Boolean getHasChilds() {
		if (hasChilds == null) {
			hasChilds = Boolean.FALSE;
		}
		return hasChilds;
	}

	public void setHasChilds(Boolean hasChilds) {
		this.hasChilds = hasChilds;
	}
	
	public Long getUidValidity() {
		return uidValidity;
	}

	public void setUidValidity(Long uidValidity) {
		this.uidValidity = uidValidity;
	}

	@Override
	public String toString() {
		return "Folder [parent=" + parent + ", account=" + account + ", name=" + name + ", label=" + label
				+ ", unreadMsgs=" + unreadMsgs + ", hasChilds=" + hasChilds + ", uidValidity=" + uidValidity + "]";
	}


	
	
}
