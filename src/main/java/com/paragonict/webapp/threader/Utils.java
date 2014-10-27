package com.paragonict.webapp.threader;

import javax.mail.Address;

public class Utils {

	public static String addressesToString(final Address[] adr) {
		if (adr == null) {
			return "";
		}
		if (adr.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Address a: adr) {
			if (sb.length() > 0 ) {
				sb.append(",");
			}
			sb.append(a.toString());
		}
		return sb.toString();
	}
}
