package com.paragonict.webapp.threader.mail;

import java.util.Comparator;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.tapestry5.grid.ColumnSort;
import org.apache.tapestry5.grid.SortConstraint;

public class MessageComparator implements Comparator<Message>{

	public final String _sortField;
	public final ColumnSort _sort;
	
	public MessageComparator(final SortConstraint sc) {
		_sortField = sc.getPropertyModel().getPropertyName();
		_sort = sc.getColumnSort();
	}

	@Override
	public int compare(Message o1, Message o2) {
		final int res = compareMe(o1, o2);
		if (_sort.equals(ColumnSort.DESCENDING)) {
			return -res; // negative sort
		}
		return res;
	}
	
	private int compareMe(Message o1, Message o2) {
		try {
			switch(_sortField) {
			case "from" : {
				final Address a1 = o1.getFrom()[0];
				final Address a2 = o2.getFrom()[0];
				return a1.toString().compareTo(a2.toString());
			}
			case "sentDate" : {
				if (o1.getSentDate()!=null && o2.getSentDate() !=null) {
					return o1.getSentDate().compareTo(o2.getSentDate());
				}
				break;
			}
			case "subject" : {
				return o1.getSubject().compareTo(o2.getSubject());
			}
			}
		}catch (MessagingException me) {
			me.printStackTrace();
		}
		return 0;
	}
}
