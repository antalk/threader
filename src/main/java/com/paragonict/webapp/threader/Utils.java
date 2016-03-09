package com.paragonict.webapp.threader;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

public class Utils {

	public static boolean contains(InternetAddress[] internetAddresses,
			String emailAddress) {

		if ((internetAddresses != null) && StringUtils.isNotBlank(emailAddress)) {
			for (int i = 0; i < internetAddresses.length; i++) {
				if (emailAddress.equals(internetAddresses[i].getAddress())) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isValid(String emailAddress) {
		return EmailValidator.getInstance().isValid(emailAddress);
	}

	public static InternetAddress[] removeEntry(Address[] addresses,
			String emailAddress) {

		InternetAddress[] internetAddresses = (InternetAddress[]) addresses;

		List<InternetAddress> list = new ArrayList<InternetAddress>();

		if ((internetAddresses == null) || StringUtils.isBlank(emailAddress)) {
			return internetAddresses;
		}

		for (int i = 0; i < internetAddresses.length; i++) {
			if (!emailAddress.equals(internetAddresses[i].getAddress())) {
				list.add(internetAddresses[i]);
			}
		}

		return list.toArray(new InternetAddress[list.size()]);
	}

	public static String toString(Address... address) {
		if (address == null) {
			return "";
		}
		if (address.length == 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder(20);

		boolean separator = false;
		for (Address adr: address) {
			InternetAddress internetAddress = (InternetAddress) adr;
			
			if (separator) sb.append(",");
			
			final String personal = internetAddress.getPersonal();
			final String emailAddress = internetAddress.getAddress();

			if (StringUtils.isNotBlank(personal)) {
				sb.append(personal);
				sb.append(' ');
				sb.append('<');
				sb.append(emailAddress);
				sb.append('>');
			} else {
				sb.append(emailAddress);
			}
			separator = true;
		}
		return sb.toString();
	}

	

	public static void validateAddresses(Address[] addresses)
			throws AddressException {

		if (addresses == null) {
			throw new AddressException();
		}

		for (Address internetAddress : addresses) {
			EmailValidator.getInstance().isValid(internetAddress.toString());
		}
	}
}
