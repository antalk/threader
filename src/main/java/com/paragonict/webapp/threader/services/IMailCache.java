package com.paragonict.webapp.threader.services;

import javax.mail.Message;
import javax.mail.MessagingException;

import com.paragonict.webapp.threader.beans.ClientMessage;

import net.sf.ehcache.CacheManager;

/**
 * a wrapper around ehcache...
 * 
 * @author avankalleveen
 *
 */
public interface IMailCache {

	/**
	 * 
	 * 
	 * @param UID
	 * @param orgMsg
	 * @param lite , get a slimed down version, only subject, date, sender and flags..
	 * @return
	 * @throws MessagingException
	 */
	public ClientMessage getMessage(final String UID, final Message orgMsg,final boolean lite) throws MessagingException;
	
	
}

