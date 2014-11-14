package com.paragonict.webapp.threader.pages.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Response;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.services.IMailService;

public class MessageContent {
	
	@SessionState
	private SessionStateObject sso;
	

	@Inject
	private IMailService ms;
	
	@OnEvent(value=EventConstants.ACTIVATE)
	private Object getMessageContents() throws MessagingException { 
		
		if ( (sso.getValue(SESSION_ATTRS.SELECTED_MSG_ID) != null &&
				sso.getValue(SESSION_ATTRS.SELECTED_FOLDER) != null)) {
		
			final Message m = ms.getMessage((String)sso.getValue(SESSION_ATTRS.SELECTED_FOLDER), (Integer)sso.getValue(SESSION_ATTRS.SELECTED_MSG_ID));
			
			return new StreamResponse() {
				
				@Override
				public void prepareResponse(Response response) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public InputStream getStream() throws IOException {
					try {
						return new ByteArrayInputStream(ms.getMessageContent(m).getBytes());
					} catch (MessagingException e) {
						throw new IOException(e);
					}
				}
				
				@Override
				public String getContentType() {
					return "text/html";
				}
			};
		}
		return com.paragonict.webapp.threader.pages.Index.class;
	}
}
