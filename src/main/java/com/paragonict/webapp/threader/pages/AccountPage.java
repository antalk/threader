package com.paragonict.webapp.threader.pages;

import org.apache.tapestry5.annotations.OnEvent;

// anonymous page
public class AccountPage {


	@OnEvent(value="accountSuccess")
	private Object getIndexPage() {
		return Index.class;
	}
}
