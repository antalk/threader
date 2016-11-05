package com.paragonict.webapp.threader.services;

public interface IMailFetcher {

	public void isInSync(final String folder,final int end, final int nrOfMsgsOnServer); 
}