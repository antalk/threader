package com.paragonict.webapp.threader.entities;


import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;


@Entity
public class Account extends AbstractIdEntity {

	public enum PROTOCOL {
		pop3,
		imap,
		pops,
		imaps;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3384482777641553645L;
	
	
	private String fullName;
	private String emailAddress;
	private String host;
	private PROTOCOL protocol;
	private String password;
	
	public Account() {
		
	}
	
	
	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getEmailAddress() {
		return emailAddress;
	}
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	@Enumerated(EnumType.STRING)
	public PROTOCOL getProtocol() {
		return protocol;
	}
	public void setProtocol(PROTOCOL protocol) {
		this.protocol = protocol;
	}
	
	
}
