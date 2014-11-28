package com.paragonict.webapp.threader.services.impl;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import com.paragonict.webapp.threader.Utils;
import com.paragonict.webapp.threader.beans.ClientMessage;
import com.paragonict.webapp.threader.services.IMailCache;


/**
 * A stand-alone service ? or integrated in {@link MailServiceImpl} ?!
 * 
 * Provides a caching wrapper around EhCache
 * 
 * @author avankalleveen
 *
 */
public class MailCacheManager implements IMailCache {

	private CacheManager _manager;
	private Cache _msgcache;
	
	@PostConstruct
	public void init() {
		_manager = CacheManager.newInstance(this.getClass().getResourceAsStream("/mailcache.xml"));
		_msgcache = _manager.getCache("mailmessages");
	}
	
	@Override
	public ClientMessage getMessage(String UID, Message orgMsg,boolean lite) throws MessagingException {
		
		Element el = _msgcache.get(UID);
		if (el == null) {
			// create ClientMessage from Message and put in cache 
			final ClientMessage cm = new ClientMessage();
			cm.setMsgId(orgMsg.getMessageNumber());
			cm.setFrom(Utils.addressesToString(orgMsg.getFrom()));
			cm.setRead(orgMsg.getFlags().contains(Flag.SEEN));
			cm.setSentDate(orgMsg.getSentDate());
			cm.setSubject(orgMsg.getSubject());
			try {
				cm.setContent(getMessageContent(orgMsg));
			} catch (IOException ioe) {
				ioe.printStackTrace();
				cm.setContent("<There was an error retrieving the message contents>");
			}
			el = new Element(UID,cm);
			_msgcache.put(el);
		}
		return (ClientMessage) el.getObjectValue();
	}
	
	/**
     * Return the primary text content of the message.
     * Copied from the interwebs
     */
	private String getMessageContent(Part p) throws IOException,MessagingException {
	    if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            //textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = getMessageContent(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getMessageContent(bp);
                    if (s != null)
                        return s;
                } else {
                    return getMessageContent(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getMessageContent(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }
        return null;
    }
}

