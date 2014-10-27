package com.paragonict.webapp.threader.services;

import javax.mail.MessagingException;
import javax.mail.Session;

import com.paragonict.webapp.threader.mail.MailStore;


public interface IMailSession {
	
	Session getSMTPSession();
		
	MailStore getStore() throws MessagingException;
}
