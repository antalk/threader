package com.paragonict.webapp.threader.components;

import java.util.List;

import org.apache.tapestry5.Block;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.hibernate.criterion.Restrictions;

import com.paragonict.tapisser.growl.Message;
import com.paragonict.tapisser.growl.Message.LEVEL;
import com.paragonict.webapp.threader.annotation.RequiresLogin;
import com.paragonict.webapp.threader.base.BaseComponent;
import com.paragonict.webapp.threader.entities.Contact;

@RequiresLogin
public class Contacts extends BaseComponent {
	
	@Inject
	private HibernateSessionManager hsm;
	
	@Component
	private Zone contactszone;

	@Property
	private Contact contact;
	
	
	public List<Contact> getContacts() {
		return hsm.getSession().createCriteria(Contact.class).add(Restrictions.eq("owner", getAccountService().getAccount())).list();
	}
	
	public Class<?> getRowType() {
		return Contact.class;
	}
	
	
	@OnEvent(value="delete")
	private Block deleteContact(Long id) {
		try {
			hsm.getSession().delete(hsm.getSession().load(Contact.class, id));
			hsm.commit();
			addGrowlerMessage(new Message("Contact deleted"));
		} catch (Exception e) {
			e.printStackTrace();
			addGrowlerMessage(new Message(LEVEL.ERROR,"Could not delete contact"));
		}
		return contactszone.getBody();
	}
}
