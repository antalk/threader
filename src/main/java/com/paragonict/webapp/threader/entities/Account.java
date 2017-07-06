package com.paragonict.webapp.threader.entities;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;

@NamedQueries({
	@NamedQuery(
			hints={@QueryHint(name="org.hibernate.readOnly",value="true")},
			name = Account.GET_ACCOUNTID,
			query = "select a.id from Account a where a.id = :accountid"
	)}
)
@Entity
public class Account extends AbstractIdEntity {

	public final static String GET_ACCOUNTID = "GetAccountId";
	
	public enum PROTOCOL {
		pop3, imap, pop3s, imaps;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3384482777641553645L;

	// account
	private String fullName;
	private String emailAddress;
	private String accountName;
	private String password;
	// incoming
	private String host;
	private Integer port;
	private PROTOCOL protocol;
	// outgoing
	private String smtpHost;
	private Integer smtpPort;
	private Boolean smtpAuth;
	private Boolean smtpTLS;

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

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		if (port == null) {
			return 25;
		}
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getSmtpHost() {
		return smtpHost;
	}

	public void setSmtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
	}

	public Integer getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(Integer smtpPort) {
		this.smtpPort = smtpPort;
	}

	public Boolean getSmtpAuth() {
		return smtpAuth;
	}

	public void setSmtpAuth(Boolean smtpAuth) {
		this.smtpAuth = smtpAuth;
	}
	public Boolean getSmtpTLS() {
		return smtpTLS;
	}

	public void setSmtpTLS(Boolean smtpTLS) {
		this.smtpTLS = smtpTLS;
	}

	@Enumerated(EnumType.STRING)
	public PROTOCOL getProtocol() {
		return protocol;
	}

	public void setProtocol(PROTOCOL protocol) {
		this.protocol = protocol;
	}

}
