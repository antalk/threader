package com.paragonict.webapp.threader.components;

import java.util.LinkedList;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentEventCallback;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventConstants;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Persist;
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
import org.chenillekit.tapestry.core.components.AjaxCheckbox;

import com.paragonict.webapp.threader.annotation.RequiresLogin;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.grid.GridMessageSource;
import com.paragonict.webapp.threader.services.IMailService;

@RequiresLogin
@Import(stylesheet="messages/messages.css")
public class Messages {
	
	@SessionState
	private SessionStateObject sso;
	
	@Inject
	private ComponentResources resources;
	
	@Inject
	private AjaxResponseRenderer arr;
	
	@Inject
	private IMailService service;
	
	@Inject
	private BeanModelSource bms;
	
	@Inject
	private AlertManager am;
	
	@Component
	private Grid messageGrid;
	
	@Property
	private LocalMessage message;
	
	@Property
	private boolean msgChecked;
	
	@Persist // persist only for THIS page, and this user..
	private List<String> selectedUIDs;
	
	@SetupRender
	private void setup() {
		if (getFolderSelected()) {
				
			// sorting by default on sentDate, can be from and subject...		
			if (messageGrid.getSortModel().getSortConstraints().isEmpty()) {
				while (messageGrid.getSortModel().getColumnSort("sentDate").compareTo(ColumnSort.DESCENDING) !=0) {
					messageGrid.getSortModel().updateSort("sentDate");
				}
			}
		}
		selectedUIDs = new LinkedList<String>();// clear
	}
	

	public boolean getFolderSelected() {
		return sso.hasValue(SESSION_ATTRS.SELECTED_FOLDER);
	}
		
	@OnEvent(value=EventConstants.PROGRESSIVE_DISPLAY)
	private Block loadMessages() throws MessagingException {
		return resources.getBlock("messageblock");
	}
	
	// ajax event.. trigger zone 'messages' to reload messages
	private Block onDeleteMessages() throws MessagingException {
		
		if (!selectedUIDs.isEmpty()) {
			
			final String currentWorkingDraftUID = sso.getStringValue(SESSION_ATTRS.DRAFT_UID);
			final String currentViewingMsgID = sso.getStringValue(SESSION_ATTRS.SELECTED_MSG_UID);
			
			service.deleteMailMessage(selectedUIDs.toArray(new String[]{}));
			
			// TODO: move this to businesslogic
			for (String UID: selectedUIDs) {
				
				if (UID.equals(currentWorkingDraftUID)) {
					sso.clearValue(SESSION_ATTRS.DRAFT_UID);
				}
				if (UID.equals(currentViewingMsgID)) {
					sso.clearValue(SESSION_ATTRS.SELECTED_MSG_UID);
				}
				
			}
			am.info("Deleted selected messages");
			//trigger ajax reload
			
			return resources.getBlock("messageblock");
		} else {
			am.info("No messages selected");
			return null;
		}
	}
	
	@OnEvent(value=AjaxCheckbox.EVENT_NAME)
	private void toggleMessage(String UID) {
		if (selectedUIDs.contains(UID)) {
			// already in , remove
			selectedUIDs.remove(UID);
		} else {
			selectedUIDs.add(UID);
		}
	}
	
	@Cached
	public GridDataSource getMessageSource() throws MessagingException {
		if (getFolderSelected()) {
			final String selectedFolder = sso.getStringValue(SESSION_ATTRS.SELECTED_FOLDER);
			return new GridMessageSource(service,selectedFolder);
		}
		return null;
	}
	
	
	/*
	 *  TODO: also check if this is the SENT items folder!!!, same type as draft, eg. outgoing !
	 *  TOOD: rename to outgoing
	 */
	@Cached
	public boolean getIsOutGoing() {
		return sso.getStringValue(SESSION_ATTRS.SELECTED_FOLDER).equalsIgnoreCase("DRAFTS");
	}
	
	public boolean getMessageRead() throws MessagingException {
		return service.isMessageRead(message);
	}
	
	/* display properties */
	public String getSubject() throws MessagingException {
		if (StringUtils.isBlank(message.getSubject())) {
			return "<No Subject>";
		}
		return message.getSubject();
	}
	
	public String getFromortoAddress() {
		if (getIsOutGoing()) {
			return message.getToAdr();
		} else {
			return message.getFromAdr();
		}
	}
	
	public String getHeaderAddress() {
		if (getIsOutGoing()) {
			return "To";
		} else {
			return "From";
		}
	}
	
	public String getMessageDate() throws MessagingException {
		if (message.getSentDate() == null) {
			return "<Unknown>";
		}
		// do this with java 8 date?
		return DateFormatUtils.SMTP_DATETIME_FORMAT.format(message.getSentDate());
	}
	
	public String getColumns() {
		if (getIsOutGoing()) {
			return "sentDate"; // TODO; get creation or lamu date here?
		} else {
			return "sentDate";
		}
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
