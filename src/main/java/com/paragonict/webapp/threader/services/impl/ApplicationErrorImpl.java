package com.paragonict.webapp.threader.services.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.tapestry5.ioc.annotations.PostInjection;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.apache.tapestry5.ioc.services.ThreadCleanupListener;

import com.paragonict.webapp.threader.services.IApplicationError;

public class ApplicationErrorImpl implements IApplicationError, ThreadCleanupListener {

	private List<String> appErrors;
	
	@PostInjection
	public void init(PerthreadManager pm) {
		appErrors = new ArrayList<>(5);
		pm.addThreadCleanupListener(this);
	}
	
	@Override
	public void addApplicationError(String msg) {
		appErrors.add(msg);
	}
	
	@Override
	public List<String> getApplicationErrors() {
		return appErrors;
	}
	
	@Override
	public void threadDidCleanup() {
		System.err.println("clearing apperrors "  + appErrors);
		
		appErrors = null;
	}
}
