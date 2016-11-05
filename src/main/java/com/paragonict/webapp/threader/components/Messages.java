package com.paragonict.webapp.threader.components;

import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentEventCallback;
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
import com.paragonict.webapp.threader.base.BaseComponent;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.LocalMessage;
import com.paragonict.webapp.threader.grid.GridMessageSource;
import com.paragonict.webapp.threader.services.IMailService;

@RequiresLogin
@Import(stylesheet="messages/messages.css")
public class Messages extends BaseComponent {
	
	@SessionState
	private SessionStateObject sso;
	
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
	private List<Long> selectedIDs;
	
	@Persist
	private String lastFolderSelected;
	
	@SetupRender
	private void setup() {
		if (getFolderSelected()) {
			
			if (!sso.getStringValue(SESSION_ATTRS.SELECTED_FOLDER).equals(lastFolderSelected)) {
				messageGrid.reset();
				lastFolderSelected = sso.getStringValue(SESSION_ATTRS.SELECTED_FOLDER);
			}
			
			
			// sorting by default on sentDate, can be from and subject...		
			if (messageGrid.getSortModel().getSortConstraints().isEmpty()) {
				while (messageGrid.getSortModel().getColumnSort("date").compareTo(ColumnSort.DESCENDING) !=0) {
					messageGrid.getSortModel().updateSort("date");
				}
			}
		}
		selectedIDs = new LinkedList<Long>();// clear
	}
	

	public boolean getFolderSelected() {
		return sso.hasValue(SESSION_ATTRS.SELECTED_FOLDER);
	}
		
	@OnEvent(value=EventConstants.PROGRESSIVE_DISPLAY)
	private Block loadMessages() throws MessagingException {
		return getResources().getBlock("messageblock");
	}
	
	// ajax event.. trigger zone 'messages' to reload messages
	private Block onDeleteMessages() throws MessagingException {
		
		if (!selectedIDs.isEmpty()) {
			
			final Long currentWorkingDraftUID = sso.getLongValue(SESSION_ATTRS.DRAFT_ID);
			final Long currentViewingMsgID = sso.getLongValue(SESSION_ATTRS.SELECTED_MSG_ID);
			
			service.deleteMailMessage(selectedIDs.toArray(new Long[]{}));
			
			// TODO: move this to businesslogic
			/*
			for (Long id: selectedIDs) {
				
				if (UID.equals(currentWorkingDraftUID)) {
					sso.clearValue(SESSION_ATTRS.DRAFT_UID);
				}
				if (UID.equals(currentViewingMsgID)) {
					sso.clearValue(SESSION_ATTRS.SELECTED_MSG_UID);
				}
				
			}
			*/
			am.info("Deleted selected messages");
			//trigger ajax reload
			
			return getResources().getBlock("messageblock");
		} else {
			am.info("No messages selected");
			return null;
		}
	}
	
	@OnEvent(value=AjaxCheckbox.EVENT_NAME)
	private void toggleMessage(Long id) {
		if (selectedIDs != null) {
			if (selectedIDs.contains(id)) {
				// already in , remove
				selectedIDs.remove(id);
			} else {
				selectedIDs.add(id);
			}
		} else {
			// session expired..
			am.warn(MessageFormat.format("Could not select msg with id {}, session invalid or expired",id));
			// TODO: make sure this message appears as response on the current AJAX request and not AFTER this!
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
		final Date calcDate = getIsOutGoing()?message.getSentDate():message.getReceivedDate();
		
		if (calcDate == null) {
			return "<Unknown>";
		} else {
			if (DateUtils.isSameDay(calcDate, new Date())) {
				return DateFormatUtils.format(calcDate, com.paragonict.webapp.threader.Constants.DAYTIME);
			} else {
				return DateFormatUtils.format(calcDate, com.paragonict.webapp.threader.Constants.YEAR_MONTH_DAY);
			}
		}
	}
	
	@OnEvent(value="fetchMessageContent")
	private void getMessageContent(Long id) {
		
		System.err.println("Read the message content");
		
		final Holder<Block> holder = new Holder<Block>();
		
		if (getResources().triggerEvent("getMessageContent", new Object[] {id}, new ComponentEventCallback<Block>() {

			public boolean handleResult(Block result) {
				holder.put(result);
				return true;
			}
			
			})) {
			arr.addRender("contentZone", holder.get());
		} 
	}
}
