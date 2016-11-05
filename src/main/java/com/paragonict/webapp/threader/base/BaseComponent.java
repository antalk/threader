package com.paragonict.webapp.threader.base;

import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.ioc.annotations.Inject;

import com.paragonict.webapp.threader.services.IAccountService;

public abstract class BaseComponent {

	@Inject
	private ComponentResources resources;
	
	@Inject
	private IAccountService as;
	
	public ComponentResources getResources() {
		return resources;
	}
	
	public IAccountService getAccountService() {
		return as;
	}
	
	public Block getAjaxLoader() {
		return resources.getPage().getComponentResources().getBlock("ajaxLoader");
	}
}
