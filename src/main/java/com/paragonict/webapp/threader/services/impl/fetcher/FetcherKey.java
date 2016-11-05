package com.paragonict.webapp.threader.services.impl.fetcher;

public class FetcherKey {

	private final String folder;
	private final Long accountId;
	
	public FetcherKey(final String folder,final Long accountId) {
		this.folder = folder;
		this.accountId = accountId;
	}
	
	public Long getAccountId() {
		return accountId;
	}
	
	public String getFolder() {
		return folder;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj instanceof FetcherKey) {
			if (((FetcherKey) obj).getAccountId().compareTo(accountId) ==0 &&
					((FetcherKey)obj).getFolder().equals(folder)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return folder.hashCode()+ accountId.hashCode();
	}
	
	@Override
	public String toString() {
		return "Account :" + accountId + " Folder: " + folder;
	}
	
}
