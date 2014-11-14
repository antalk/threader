package com.paragonict.webapp.threader.components;

import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
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

import com.paragonict.webapp.threader.beans.ClientMessage;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.grid.GridMessageSource;
import com.paragonict.webapp.threader.services.IMailService;

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
	private ClientMessage message;
	
	@SetupRender
	private void setup() {
		if (messageGrid.getSortModel().getSortConstraints().isEmpty()) {
			while (messageGrid.getSortModel().getColumnSort("sentDate").compareTo(ColumnSort.DESCENDING) !=0) {
				messageGrid.getSortModel().updateSort("sentDate");
			}
		}
	}
	
	public boolean getFolderSelected() {
		return sso.getValue(SESSION_ATTRS.SELECTED_FOLDER) != null;
	}

	
	@OnEvent(value=EventConstants.PROGRESSIVE_DISPLAY)
	private Block loadMessages() throws MessagingException {
		//getMessageSource();
		return resources.getBlock("messageblock");
	}
	
	@Cached
	public GridDataSource getMessageSource() throws MessagingException {
		final String selectedFolder = (String)sso.getValue(SESSION_ATTRS.SELECTED_FOLDER);
		//int endIndex = ms.getNrOfMessages(selectedFolder);
		return new GridMessageSource(ms,selectedFolder);
	}
	
	/*
	@Cached
	public BeanModel<Message> getMessageModel() {
		return bms.createDisplayModel(Message.class, resources.getMessages());
	}*/
	
	
	public String getSubject() throws MessagingException {
		if (StringUtils.isBlank(message.getSubject())) {
			return "<No Subject>";
		}
		return message.getSubject();
	}
	
	
	@OnEvent(value="fetchMessageContent")
	private void refreshFolderContent(Integer id) {
		
		System.err.println("Refresh the message content");
		
		final Holder<Block> holder = new Holder<Block>();
		
		if (resources.triggerEvent("getMessageContent", new Object[] {id}, new ComponentEventCallback<Block>() {

			public boolean handleResult(Block result) {
				holder.put(result);
				return true;
			}
			
			})) {
			arr.addRender("contentZone", holder.get());
		} 
	}
}
