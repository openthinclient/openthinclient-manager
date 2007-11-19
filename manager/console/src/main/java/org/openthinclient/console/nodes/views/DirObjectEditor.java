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
 *******************************************************************************/
package org.openthinclient.console.nodes.views;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

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
import org.openthinclient.console.nodes.DirObjectNode;
import org.openthinclient.console.nodes.RealmNode;
import org.openthinclient.console.ui.AssociationEditor;
import org.openthinclient.console.util.ChildValidator;
import org.openthinclient.console.util.DetailViewFormBuilder;

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

	/*
	 * @see com.jgoodies.validation.Validator#validate()
	 */
	public ValidationResult validate() {
		model.triggerCommit();

		PropertyValidationSupport support = new PropertyValidationSupport(
				dirObject, dirObject.getClass().getSimpleName()); //$NON-NLS-1$

		if (dirObject.getName().length() == 0)
			support
					.addError(
							"name", Messages.getString("DirObjectEditor.validation.name.mandatory")); //$NON-NLS-1$ //$NON-NLS-2$

		for (char c : dirObject.getName().toCharArray())
			// FIXME: due to ADS limitation: discourage anything but letters&digits
			// if (Character.isISOControl(c) || ",=\\".indexOf(c) >= 0) {
			if (!(Character.isLetterOrDigit(c) || "-_/+?!".indexOf(c) >= 0)) {

				support
						.addWarning(
								"name", Messages.getString("DirObjectEditor.validation.name.discouraged")); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			}

		return support.getResult();
	}

	@Override
	public JComponent getHeaderComponent() {
		boolean isMutabel = true;
		boolean isRealm = false;
		String name = dirObject.getClass().getSimpleName();

		// check if is mutable
		if (!LDAPDirectory.isMutable(dirObject.getClass())) {
			isMutabel = false;
		}
		if (name.equals("Realm")) {
			isRealm = true;
		}

		JPanel panel = new JPanel();

		final PresentationModel model = new PresentationModel(new ValueHolder(
				dirObject, true));

		DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
				"p, 10dlu, r:p, 3dlu, f:p:g"), Messages.getBundle(), panel); //$NON-NLS-1$
		dfb.setLeadingColumnOffset(2);
		dfb.setColumn(3);

		if (isMutabel == false) {
			dfb.appendI15d("Label_name", BasicComponentFactory.createLabel(model
					.getModel("name")));
			dfb.appendI15d("Label_description", BasicComponentFactory
					.createLabel(model.getModel("description")));
		} else if (isRealm == true) {
			String dn = realm.getConnectionDescriptor().getBaseDN();
			dn = dn.replace("\\,", "#%COMMA%#");
			String[] s = dn.split(",");
			String nameRealm = "";
			if (s.length > 0) {
				nameRealm = s[0].replace("ou=", "").trim();
				nameRealm = nameRealm.replace("#%COMMA%#", "\\,").trim();
			}
			JTextField field = new javax.swing.JTextField();
			field.setText(nameRealm);
			dfb.appendI15d("Label_name", field); //$NON-NLS-1$
			dfb.appendI15d(
					"Label_description", BasicComponentFactory.createTextField( //$NON-NLS-1$
							model.getModel("description"), true)); //$NON-NLS-1$
		} else {
			dfb.appendI15d("Label_name", BasicComponentFactory.createTextField( //$NON-NLS-1$
					model.getModel("name"), false)); //$NON-NLS-1$
			dfb.appendI15d(
					"Label_description", BasicComponentFactory.createTextField( //$NON-NLS-1$
							model.getModel("description"), true)); //$NON-NLS-1$
		}

		panel.add(new JLabel(IconManager.getInstance(DetailViewProvider.class,
				"icons").getIcon("tree." + dirObject.getClass().getSimpleName())), //$NON-NLS-1$ //$NON-NLS-2$
				new CellConstraints(1, 1, 1, dfb.getRowCount(), CellConstraints.CENTER,
						CellConstraints.TOP));

		panel.putClientProperty(DirObjectEditor.KEY_VALIDATOR, this);

		return panel;
	} /*
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

		for (Node node : selection) {
			if (node instanceof DirObjectNode) {
				realm = (Realm) node.getLookup().lookup(Realm.class);
				dirObject = (DirectoryObject) node.getLookup().lookup(
						DirectoryObject.class);
			} else if (node instanceof RealmNode) {
				dirObject = realm = (Realm) node.getLookup().lookup(Realm.class);
			}
		}

		if (null == dirObject)
			throw new IllegalStateException(
					"Could not lookup a DirectoryObject instance"); //$NON-NLS-1$
		if (null == realm)
			throw new IllegalStateException("Could not lookup a Realm instance"); //$NON-NLS-1$

		model = new PresentationModel(new ValueHolder(dirObject, true));
	}

	public static JComponent getEditorForDirObject(DirectoryObject dirObject,
			Realm realm) {

		JTabbedPane tabbedPane = new JTabbedPane();
		ChildValidator validator = new ChildValidator();
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

			Set<User> users = realm.getAdministrators().getMembers();
			final AssociationEditor associationEditor = new AssociationEditor(users,
					realm, User.class, realm.getAdministrators(),
					AssociationEditor.TYPE_MEMBERS);
			associationEditor.setName(Messages
					.getString("DirObjectDetailView.administrators")); //$NON-NLS-1$
			addSubView(tabbedPane, associationEditor, validator);
		}

		if (dirObject instanceof AssociatedObjectsProvider) {
			Map<Class, Set<? extends DirectoryObject>> subgroups = ((AssociatedObjectsProvider) dirObject)
					.getAssociatedObjects();
			if (subgroups != null) {
				Set<Class> keys = subgroups.keySet();
				for (Class memberClass : keys) {
					addSubView(tabbedPane, new AssociationEditor(subgroups
							.get(memberClass), realm, memberClass, dirObject,
							AssociationEditor.TYPE_ASSOC_OBJECTS), validator);
				}
			}
		}
		if (dirObject instanceof Group) {
			Set<DirectoryObject> members = ((Group) dirObject).getMembers();
			Map<Class, Set<DirectoryObject>> referrers = new HashMap<Class, Set<DirectoryObject>>();
			if (members != null) {
				if (members.size() != 0) {
					for (DirectoryObject member : members) {
						if (referrers.containsKey(member.getClass())) {
							Set<DirectoryObject> set = referrers.get(member.getClass());
							set.add(member);
						} else {
							Set<DirectoryObject> set = new HashSet<DirectoryObject>();
							set.add(member);
							referrers.put(member.getClass(), set);
						}
					}
				}
			}

			// add subview for classes which have no members yet
			Class[] memberClasses = ((Group) dirObject).getMemberClasses();
			for (int i = 0; i < memberClasses.length; i++) {
				if (!referrers.containsKey(memberClasses[i])) {
					referrers.put(memberClasses[i], new HashSet<DirectoryObject>());
				}
			}
			Set<Class> keys = referrers.keySet();
			for (Class key : keys)
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

		JScrollPane scrollPane = new JScrollPane(subView);
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
