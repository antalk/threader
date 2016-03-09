package com.paragonict.webapp.threader.components;

import javax.mail.Flags.Flag;
import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentEventCallback;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.annotations.SetupRender;
import org.apache.tapestry5.corelib.components.Grid;
import org.apache.tapestry5.grid.ColumnSort;
import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.internal.util.Holder;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;

import com.paragonict.webapp.threader.annotation.RequiresLogin;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.grid.GridMessageSource;
import com.paragonict.webapp.threader.services.IMailService;

@RequiresLogin
public class Messages {
	
	@SessionState
	private SessionStateObject sso;
	
	@Inject
	private ComponentResources resources;
	
	@Inject
	private AjaxResponseRenderer arr;
	
	@Inject
	private IMailService ms;
	
	@Inject
	private BeanModelSource bms;
	
	@Component
	private Grid messageGrid;
	
	@Property
	private LocalMessage message;
	
	@SetupRender
	private void setup() {
		
// sorting by default on sentDate, can be from and subject...		
		if (messageGrid.getSortModel().getSortConstraints().isEmpty()) {
			while (messageGrid.getSortModel().getColumnSort("sentDate").compareTo(ColumnSort.DESCENDING) !=0) {
				messageGrid.getSortModel().updateSort("sentDate");
			}
		}
	}
	
	public boolean getFolderSelected() {
		return sso.hasValue(SESSION_ATTRS.SELECTED_FOLDER);
	}
		
	@OnEvent(value=EventConstants.PROGRESSIVE_DISPLAY)
	private Block loadMessages() throws MessagingException {
		//getMessageSource();
		return resources.getBlock("messageblock");
	}
	
	@Cached
	public GridDataSource getMessageSource() throws MessagingException {
		final String selectedFolder = sso.getStringValue(SESSION_ATTRS.SELECTED_FOLDER);
		//int endIndex = ms.getNrOfMessages(selectedFolder);
		return new GridMessageSource(ms,selectedFolder);
	}
	
	@Cached
	public boolean getIsDraftFolder() {
		return sso.getStringValue(SESSION_ATTRS.SELECTED_FOLDER).equalsIgnoreCase("DRAFTS");
	}
	
	public boolean getMessageRead() throws MessagingException {
		return ms.isMessageRead(message);
	}
	
	/* display properties */
	public String getSubject() throws MessagingException {
		if (StringUtils.isBlank(message.getSubject())) {
			return "<No Subject>";
		}
		return message.getSubject();
	}
	
	public String getMessageDate() throws MessagingException {
		if (message.getSentDate() == null) {
			return "<Unknown>";
		}
		
		return DateFormatUtils.ISO_DATETIME_FORMAT.format(message.getSentDate());
	}
	
	
	@OnEvent(value="fetchMessageContent")
	private void getMessageContent(String UID) {
		
		System.err.println("Read the message content");
		
		final Holder<Block> holder = new Holder<Block>();
		
		if (resources.triggerEvent("getMessageContent", new Object[] {UID}, new ComponentEventCallback<Block>() {

			public boolean handleResult(Block result) {
				holder.put(result);
				return true;
			}
			
			})) {
			arr.addRender("contentZone", holder.get());
		} 
	}
}
