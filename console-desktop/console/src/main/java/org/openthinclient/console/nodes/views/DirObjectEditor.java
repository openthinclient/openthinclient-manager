/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.console.nodes.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.AssociatedObjectsProvider;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Group;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.common.model.User;
import org.openthinclient.console.AbstractDetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.Messages;
import org.openthinclient.console.ValidateNames;
import org.openthinclient.console.nodes.DirObjectNode;
import org.openthinclient.console.nodes.RealmNode;
import org.openthinclient.console.ui.AssociationEditor;
import org.openthinclient.console.util.ChildValidator;
import org.openthinclient.console.util.DetailViewFormBuilder;
import org.openthinclient.ldap.DirectoryException;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.Validator;
import com.jgoodies.validation.util.PropertyValidationSupport;
import com.levigo.util.swing.IconManager;

/**
 * @author Natalie Bohnert
 */
public class DirObjectEditor extends AbstractDetailView implements Validator {
	public static final String KEY_VALIDATOR = "validator"; //$NON-NLS-1$

	private DirectoryObject dirObject;

	private Realm realm;

	private PresentationModel model;

	private ArrayList<String> existingNames;

	private String oldName;

	private DirContext getContext(Realm realm) throws NamingException {
		final Hashtable env = new Hashtable();
		env
				.put(Context.INITIAL_CONTEXT_FACTORY,
						"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, realm.getConnectionDescriptor().getLDAPUrl());
		return new InitialDirContext(env);
	}

	/*
	 * @see com.jgoodies.validation.Validator#validate()
	 */
	public ValidationResult validate() {
		model.triggerCommit();

		if (oldName == null)
			oldName = dirObject.getName();

		final PropertyValidationSupport support = new PropertyValidationSupport(
				dirObject, dirObject.getClass().getSimpleName()); //$NON-NLS-1$

		final ValidateNames validate = new ValidateNames();
		final String name = dirObject.getName();
		final Class<? extends DirectoryObject> dirObjectClass = dirObject
				.getClass();
		final String result = validate.validate(name, dirObjectClass);

		if (result != null)
			support.addError("name", result);

		if (existingNames == null) {
			existingNames = new ArrayList<String>();

			try {
				final DirContext ctx = getContext(realm);
				try {
					final String ouName = realm.getDirectory().getMapping().getTypes()
							.get(dirObjectClass).getBaseRDN();

					if (ouName != null) {
						final Attributes att = new BasicAttributes(true);
						att.put(new BasicAttribute("cn"));

						final NamingEnumeration ne = ctx.search(ouName, att);

						while (ne.hasMoreElements()) {
							final SearchResult sr = (SearchResult) ne.next();
							final Attributes srName = sr.getAttributes();
							existingNames.add(srName.get("cn").get().toString());
						}
					}
				} catch (final DirectoryException e) {
					e.printStackTrace();
					ErrorManager.getDefault().notify(e);
				} finally {
					ctx.close();
				}
			} catch (final NamingException e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
			}
		}

		if (existingNames.contains(name) && !name.equals(oldName))
			support.addError("name", Messages
					.getString("DirObjectEditor.name.exists"));

		return support.getResult();
	}

	@Override
	public JComponent getHeaderComponent() {
		boolean isMutabel = true;
		boolean isRealm = false;
		final String name = dirObject.getClass().getSimpleName();

		// check if is mutable
		if (!LDAPDirectory.isMutable(dirObject.getClass()))
			isMutabel = false;
		if (name.equals("Realm"))
			isRealm = true;

		final JPanel panel = new JPanel();

		final PresentationModel model = new PresentationModel(new ValueHolder(
				dirObject, true));

		final DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
				"p, 10dlu, r:p, 3dlu, f:p:g"), Messages.getBundle(), panel); //$NON-NLS-1$
		dfb.setLeadingColumnOffset(2);
		dfb.setColumn(3);

		if (isMutabel == false) {
			dfb.appendI15d("DirObjectEditor.name", BasicComponentFactory
					.createLabel(model.getModel("name")));
			dfb.appendI15d("DirObjectEditor.description", BasicComponentFactory
					.createLabel(model.getModel("description")));
		} else if (isRealm == true) {
			// FIXME: make it easier
			String dn = realm.getConnectionDescriptor().getBaseDN();
			dn = dn.replace("\\,", "#%COMMA%#");
			final String[] s = dn.split(",");
			String nameRealm = "";
			if (s.length > 0) {
				nameRealm = s[0].replace("ou=", "").trim();
				nameRealm = nameRealm.replace("#%COMMA%#", "\\,").trim();
			}
			final JTextField field = new javax.swing.JTextField();
			field.setText(nameRealm);
			dfb.appendI15d("DirObjectEditor.name", field); //$NON-NLS-1$
			dfb.appendI15d(
					"DirObjectEditor.description", BasicComponentFactory.createTextField( //$NON-NLS-1$
							model.getModel("description"), true)); //$NON-NLS-1$
		} else {
			dfb.appendI15d(
					"DirObjectEditor.name", BasicComponentFactory.createTextField( //$NON-NLS-1$
							model.getModel("name"), false)); //$NON-NLS-1$
			dfb.appendI15d(
					"DirObjectEditor.description", BasicComponentFactory.createTextField( //$NON-NLS-1$
							model.getModel("description"), true)); //$NON-NLS-1$
		}

		panel.add(new JLabel(IconManager.getInstance(DetailViewProvider.class,
				"icons").getIcon("tree." + dirObject.getClass().getSimpleName())), //$NON-NLS-1$ //$NON-NLS-2$
				new CellConstraints(1, 1, 1, dfb.getRowCount(), CellConstraints.CENTER,
						CellConstraints.TOP));

		panel.putClientProperty(DirObjectEditor.KEY_VALIDATOR, this);

		return panel;
	}

	/*
	 * @see org.openthinclient.console.DetailView#getMainComponent()
	 */
	public JComponent getMainComponent() {
		return getEditorForDirObject(dirObject, realm);
	}

	/*
	 * @see org.openthinclient.console.DetailView#init(org.openide.nodes.Node[],
	 *      org.openide.windows.TopComponent)
	 */
	public void init(Node[] selection, TopComponent tc) {
		dirObject = realm = null;

		for (final Node node : selection)
			if (node instanceof DirObjectNode) {
				realm = (Realm) node.getLookup().lookup(Realm.class);
				dirObject = (DirectoryObject) node.getLookup().lookup(
						DirectoryObject.class);
			} else if (node instanceof RealmNode)
				dirObject = realm = (Realm) node.getLookup().lookup(Realm.class);

		if (null == dirObject)
			throw new IllegalStateException(
					"Could not lookup a DirectoryObject instance"); //$NON-NLS-1$
		if (null == realm)
			throw new IllegalStateException("Could not lookup a Realm instance"); //$NON-NLS-1$

		model = new PresentationModel(new ValueHolder(dirObject, true));
	}

	public static JComponent getEditorForDirObject(DirectoryObject dirObject,
			Realm realm) {

		final JTabbedPane tabbedPane = new JTabbedPane();
		final ChildValidator validator = new ChildValidator();
		tabbedPane.putClientProperty(DirObjectEditor.KEY_VALIDATOR, validator);

		if (dirObject instanceof User)
			addSubView(tabbedPane, new UserEditor((User) dirObject, realm), validator);

		if (dirObject instanceof Client)
			addSubView(tabbedPane, new ClientEditor((Client) dirObject, realm),
					validator);

		if (dirObject instanceof UnrecognizedClient)
			addSubView(tabbedPane, new UnrecognizedClientEditor(
					(UnrecognizedClient) dirObject, realm), validator);

		if (dirObject instanceof Profile)
			addSubView(tabbedPane, new ProfileEditor((Profile) dirObject, realm),
					validator);

		if (dirObject instanceof Realm) {
			addSubView(tabbedPane, new RealmEditor(realm), validator);

			final Set<User> users = realm.getAdministrators().getMembers();
			final AssociationEditor associationEditor = new AssociationEditor(users,
					realm, User.class, realm.getAdministrators(),
					AssociationEditor.TYPE_MEMBERS);
			associationEditor.setName(Messages
					.getString("DirObjectDetailView.administrators")); //$NON-NLS-1$
			addSubView(tabbedPane, associationEditor, validator);
		}

		if (dirObject instanceof AssociatedObjectsProvider) {
			final Map<Class, Set<? extends DirectoryObject>> subgroups = ((AssociatedObjectsProvider) dirObject)
					.getAssociatedObjects();
			if (subgroups != null) {
				final Set<Class> keys = subgroups.keySet();
				for (final Class memberClass : keys)
					addSubView(tabbedPane, new AssociationEditor(subgroups
							.get(memberClass), realm, memberClass, dirObject,
							AssociationEditor.TYPE_ASSOC_OBJECTS), validator);
			}
		}
		if (dirObject instanceof Group) {
			final Set<DirectoryObject> members = ((Group) dirObject).getMembers();
			final Map<Class, Set<DirectoryObject>> referrers = new HashMap<Class, Set<DirectoryObject>>();
			if (members != null)
				if (members.size() != 0)
					for (final DirectoryObject member : members) {
						if (member.getClass().equals(dirObject.getClass()))
							continue;
						if (referrers.containsKey(member.getClass())) {
							final Set<DirectoryObject> set = referrers.get(member.getClass());
							set.add(member);
						} else {
							final Set<DirectoryObject> set = new HashSet<DirectoryObject>();
							set.add(member);
							referrers.put(member.getClass(), set);
						}
					}

			// add subview for classes which have no members yet
			final Class[] memberClasses = ((Group) dirObject).getMemberClasses();
			for (int i = 0; i < memberClasses.length; i++)
				if (!referrers.containsKey(memberClasses[i])
						&& !dirObject.getClass().equals(memberClasses[i]))
					referrers.put(memberClasses[i], new HashSet<DirectoryObject>());
			final Set<Class> keys = referrers.keySet();
			for (final Class key : keys)
				addSubView(tabbedPane, new AssociationEditor(referrers.get(key), realm,
						key, dirObject, AssociationEditor.TYPE_MEMBERS), validator);

		}

		return tabbedPane;
	}

	/**
	 * @param subViews
	 * @param subView
	 * @param validator
	 */
	private static void addSubView(JTabbedPane tabbedPane, JComponent subView,
			ChildValidator validator) {

		final JScrollPane scrollPane = new JScrollPane(subView);
		scrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createLineBorder(UIManager.getColor("control"), 3), BorderFactory //$NON-NLS-1$
				.createLineBorder(UIManager.getColor("controlShadow")))); //$NON-NLS-1$
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.setPreferredSize(scrollPane.getMinimumSize());

		tabbedPane.addTab(subView.getName(), scrollPane);
		subView.setVisible(true);
		validator.addValidatorFrom(subView);

	}
}
