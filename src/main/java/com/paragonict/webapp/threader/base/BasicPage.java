package com.paragonict.webapp.threader.base;


import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.slf4j.Logger;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.services.IApplicationError;

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
	
	@Inject
	private AlertManager am;
	
	@Property
	@Inject
	private IApplicationError appErrors;
	
	@SessionState
	private SessionStateObject sso;
	
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
	
	public AlertManager getAm() {
		return am;
	}

	public Object handleEventContext(final EventContext ec) {
		return null;
	}
}
