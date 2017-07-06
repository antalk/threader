package com.paragonict.webapp.threader.pages.app;

import java.util.List;

import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.hibernate.criterion.Restrictions;

import com.paragonict.tapisser.growl.Message;
import com.paragonict.tapisser.growl.Message.LEVEL;
import com.paragonict.webapp.threader.base.AuthenticatedPage;
import com.paragonict.webapp.threader.entities.Contact;

public class Contacts extends AuthenticatedPage {
	
	@Property
	private Contact contact;
	
	public List<Contact> getContacts() {
		return getHsm().getSession().createCriteria(Contact.class).add(Restrictions.eq("owner", getAs().getAccount())).list();
	}
	
	public Class<?> getRowType() {
		return Contact.class;
	}
	
	
	@OnEvent(value="delete")
	private void deleteContact(Long id) {
		try {
			getHsm().getSession().delete(getHsm().getSession().load(Contact.class, id));
			getHsm().commit();
			addGrowlerMessage(new Message("Contact deleted"));
		} catch (Exception e) {
			e.printStackTrace();
			addGrowlerMessage(new Message(LEVEL.ERROR,"Could not delete contact"));
		}
	}
}
