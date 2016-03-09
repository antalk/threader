package com.paragonict.webapp.threader.pages.app;

import com.paragonict.webapp.threader.annotation.RequiresLogin;

@RequiresLogin
public class MessageContent {
	/*
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
					try {
						if (m.isMimeType("text/plain")) {
							return "text/plain";
						}
					} catch (MessagingException e) {
						e.printStackTrace();
					}
					return "text/html";
				}
			};
		}
		return new TextStreamResponse("text/plain", "No message selected");
	}*/
}
