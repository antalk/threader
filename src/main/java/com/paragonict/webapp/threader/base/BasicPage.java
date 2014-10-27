package com.paragonict.webapp.threader.base;


import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.PageRenderLinkSource;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject;

/*
 * A page which provides a lot of injected services 
 * 
 */
public abstract class BasicPage {
	
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
	
	

}
