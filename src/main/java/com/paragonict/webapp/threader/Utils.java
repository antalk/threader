package com.paragonict.webapp.threader;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

public class Utils {

	public static String addressesToString(final Address[] adr) {
		if (adr == null) {
			return "";
		}
		if (adr.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Address a : adr) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(a.toString());
		}
		return sb.toString();
	}

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

	public static String toString(Address address) {
		InternetAddress internetAddress = (InternetAddress) address;

		if (internetAddress != null) {
			StringBuilder sb = new StringBuilder(5);

			String personal = internetAddress.getPersonal();
			String emailAddress = internetAddress.getAddress();

			if (StringUtils.isNotBlank(personal)) {
				sb.append(personal);
				sb.append(' ');
				sb.append('<');
				sb.append(emailAddress);
				sb.append('>');
			} else {
				sb.append(emailAddress);
			}

			return sb.toString();
		}

		return "";
	}

	public static String toString(Address[] addresses) {
		if (ArrayUtils.isEmpty(addresses)) {
			return "";
		}

		StringBuilder sb = new StringBuilder(addresses.length * 2 - 1);

		for (int i = 0; i < (addresses.length - 1); i++) {
			sb.append(toString(addresses[i]));
			sb.append(",");
		}

		sb.append(toString(addresses[addresses.length - 1]));

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
