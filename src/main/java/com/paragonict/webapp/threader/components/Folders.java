package com.paragonict.webapp.threader.components;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.mail.MessagingException;

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

import com.paragonict.webapp.threader.annotation.RequiresLogin;
import com.paragonict.webapp.threader.base.BaseComponent;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject;
import com.paragonict.webapp.threader.beans.sso.SessionStateObject.SESSION_ATTRS;
import com.paragonict.webapp.threader.entities.Folder;
import com.paragonict.webapp.threader.services.IMailService;

@RequiresLogin
public class Folders extends BaseComponent {
	
	@Inject
	private HibernateSessionManager hsm;
	
	@Inject
	private IMailService ms;
	
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
		sso.clearValue(SESSION_ATTRS.SELECTED_FOLDER);
		getResources().triggerEvent("clearFolderZone", new Object[]{}, null);
	}
	
	
	@Cached
	public TreeModel<Folder> getFolderModel() throws MessagingException {
		
		final ValueEncoder<Folder> folderValueEncoder = new ValueEncoder<Folder>() {

			public String toClient(Folder value) {
				return Long.toString(value.getId());
			}

			public Folder toValue(String clientValue) {
				// clientvalue is the id
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
				return hsm.getSession().createCriteria(Folder.class).add(Restrictions.eq("account",getAccountService().getAccount())).add(Restrictions.eq("parent", value)).list();
			}

			public String getLabel(Folder value) {
				return value.getName();
			}
		};
		
		final Folder root = ms.getFolders(false);
		
		final List<Folder> roots= hsm.getSession().createCriteria(Folder.class).add(Restrictions.eq("account",getAccountService().getAccount())).add(Restrictions.eq("parent", root)).list();

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
		if (!sso.hasValue(SESSION_ATTRS.SELECTED_FOLDER)) {
			sso.putValue(SESSION_ATTRS.SELECTED_FOLDER, roots.get(0).getName());
		}
		
		return  new DefaultTreeModel(folderValueEncoder,folderModelAdapter,roots);
	}
	
	public boolean getHasUnreadMsgs() {
		return currentFolder.getUnreadMsgs() > 0;
	}
	
	public String getFolderSelected() {
		return (currentFolder.getName().equals(sso.getStringValue(SESSION_ATTRS.SELECTED_FOLDER))?"selected":"notselected");
	}
	
	public String getFolderId() {
		return "folder"+ currentFolder.getName();
	}
}
