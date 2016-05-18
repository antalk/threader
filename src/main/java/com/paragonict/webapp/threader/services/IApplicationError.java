package com.paragonict.webapp.threader.services;

import java.util.List;

public interface IApplicationError {
	
	public void addApplicationError(final String msg);
	
	public List<String> getApplicationErrors();

}
