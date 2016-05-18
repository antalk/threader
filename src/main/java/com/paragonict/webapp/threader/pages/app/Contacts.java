package com.paragonict.webapp.threader.pages.app;

import java.util.List;

import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.criterion.Restrictions;

import com.paragonict.webapp.threader.base.AuthenticatedPage;
import com.paragonict.webapp.threader.entities.Contact;

public class Contacts extends AuthenticatedPage {
	
	@Inject
	private AlertManager am;

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
			am.info("Contact deleted");
		} catch (Exception e) {
			e.printStackTrace();
			am.error("Could not delete contact");
		}
	}
}
