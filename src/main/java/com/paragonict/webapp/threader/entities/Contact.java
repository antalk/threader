package com.paragonict.webapp.threader.entities;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

@Entity
public class Contact extends AbstractIdEntity {

	private static final long serialVersionUID = 5701994891097659999L;

	private Account owner;
	
	private String name;
	private String mailAddress;
	
	@OneToOne
	public Account getOwner() {
		return owner;
	}
	public void setOwner(Account owner) {
		this.owner = owner;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getMailAddress() {
		return mailAddress;
	}
	public void setMailAddress(String mailAddress) {
		this.mailAddress = mailAddress;
	}
	
	
	
}
