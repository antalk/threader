package com.paragonict.webapp.threader.services;

import com.paragonict.webapp.threader.entities.Account;

public interface IAccountService {

	public boolean isLoggedIn();
	
	public Account getAccount();
}
