package com.paragonict.webapp.threader.components;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.mail.MessagingException;


import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.ValueEncoder;
import org.apache.tapestry5.alerts.AlertManager;
import org.apache.tapestry5.annotations.Cached;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.OnEvent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.corelib.components.Tree;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.tree.DefaultTreeModel;
import org.apache.tapestry5.tree.TreeModel;
import org.apache.tapestry5.tree.TreeModelAdapter;
import org.hibernate.criterion.Restrictions;

import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.Folder;
import com.paragonict.webapp.threader.entities.Message;
import com.paragonict.webapp.threader.services.IAccountService;
import com.paragonict.webapp.threader.services.IMailService;

public class Folders {
	
	@Inject
	private HibernateSessionManager hsm;
	
	@Inject
	private ComponentResources resources;
	
	@Inject
	private IMailService ms;
	
	@Inject
	private IAccountService as;
	
	@Inject
	private AjaxResponseRenderer arr;
	
	@Inject 
	private AlertManager am;
	
	@SessionState
	private SessionStateObject sso;

	@Component
	private Tree tree;
	
	@Property
	private Folder currentFolder;
	
	@OnEvent(value="reloadfolders")
	private void refreshFolderList() throws MessagingException {
		ms.getFolders(true);
		resources.triggerEvent("clearFolderZone", new Object[]{}, null);
	}
	
	@OnEvent(value="configAccount")
	private Block getAccountEditor() {
		return resources.getBlock("accountBlock");
	}
	
	@OnEvent(value="composeMessage")
	private Block getMessageEditor() {
		final Message newMessage = new Message();
		newMessage.setAccount(as.getAccount().getId());
		newMessage.setFromAdr(as.getAccount().getEmailAddress());
		
		hsm.getSession().saveOrUpdate(newMessage);
		hsm.commit();
		return resources.getBlock("composeBlock");
	}
	
	@Cached
	public TreeModel<Folder> getFolderModel() throws MessagingException {
		
		final ValueEncoder<Folder> folderValueEncoder = new ValueEncoder<Folder>() {

			public String toClient(Folder value) {
				return Long.toString(value.getId());
			}

			public Folder toValue(String clientValue) {
				// clientvalue is the uuid
				// pff.. hmm
				return (Folder) hsm.getSession().load(Folder.class, Long.parseLong(clientValue));
			}
			
		};
		
		TreeModelAdapter<Folder> folderModelAdapter = new TreeModelAdapter<Folder>() {
			
			public boolean isLeaf(Folder value) {
				return !value.getHasChilds();
			}

			public boolean hasChildren(Folder value) {
				return value.getHasChilds();
			}

			public List<Folder> getChildren(Folder value) {
				return hsm.getSession().createCriteria(Folder.class).add(Restrictions.eq("account",as.getAccount())).add(Restrictions.eq("parent", value)).list();
			}

			public String getLabel(Folder value) {
				return value.getLabel();
			}
		};
		
		final Folder root = ms.getFolders(false);
		
		final List<Folder> roots= hsm.getSession().createCriteria(Folder.class).add(Restrictions.eq("account",as.getAccount())).add(Restrictions.eq("parent", root)).list();

		Collections.sort(roots, new Comparator<Folder>() {

			@Override
			public int compare(Folder o1, Folder o2) {
				// sort inbox as first..
				
				if (o1.getName().equalsIgnoreCase("inbox")) {
					return -1;
				}
				if (o2.getName().equalsIgnoreCase("inbox")) {
					return 1;
				}
				return 0;
			}
		});
		
		// just select the FIRST folder as default, if nothgin selected
		if (sso.getValue(SESSION_ATTRS.SELECTED_FOLDER) == null) {
			sso.putValue(SESSION_ATTRS.SELECTED_FOLDER, roots.get(0).getName());
		}
		
		return  new DefaultTreeModel(folderValueEncoder,folderModelAdapter,roots);
	}
	
	public boolean getHasUnreadMsgs() {
		return currentFolder.getUnreadMsgs() > 0;
	}
	
	public String getFolderSelected() {
		return (currentFolder.getName().equals(sso.getValue(SESSION_ATTRS.SELECTED_FOLDER))?"selected":"notselected");
	}
	
	public String getFolderId() {
		return "folder"+ currentFolder.getName();
	}
}
