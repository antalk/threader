package com.paragonict.webapp.threader.base;


import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.slf4j.Logger;

import com.paragonict.tapisser.components.Growler;
import com.paragonict.tapisser.growl.Message;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;

/*
 * A page which provides a lot of injected services 
 * 
 */
public abstract class BasicPage {
	
	@Inject
	private Logger logger;
	
	@Inject
	private HibernateSessionManager hsm;

	@Inject
	private ComponentResources resources;
	
	@Inject
	private AjaxResponseRenderer arr;
	
	@Inject
	private PageRenderLinkSource prls;
	
	@SessionState
	private SessionStateObject sso;
	
	@Component
	private Growler growl;
	
	@Component
	private Zone growlZone;
	
	private boolean growlerAdded = false;
	
	private final Object onActivate(final EventContext ec) {
		
		return handleEventContext(ec);
	}

	public HibernateSessionManager getHsm() {
		return hsm;
	}

	public void setHsm(HibernateSessionManager hsm) {
		this.hsm = hsm;
	}

	public ComponentResources getResources() {
		return resources;
	}

	public void setResources(ComponentResources resources) {
		this.resources = resources;
	}

	public AjaxResponseRenderer getArr() {
		return arr;
	}

	public void setArr(AjaxResponseRenderer arr) {
		this.arr = arr;
	}

	public PageRenderLinkSource getPrls() {
		return prls;
	}

	public void setPrls(PageRenderLinkSource prls) {
		this.prls = prls;
	}

	public SessionStateObject getSso() {
		return sso;
	}

	public void setSso(SessionStateObject sso) {
		this.sso = sso;
	}
	
	public Logger getLogger() {
		return logger;
	}
	
	public Object handleEventContext(final EventContext ec) {
		return null;
	}
	
	public void addGrowlerMessage(final Message msg) {
		if (!growlerAdded) {
			// if you are getting access to the growler,, add it to the ajaxresponse renderer.. 
			// coz you probably want to show your added messages ?!
			
			System.err.println("Activating growler");
			
			arr.addRender(growlZone);
			growlerAdded = true; // true for this thread/request

		}
		growl.addMessage(msg);
	}
}
