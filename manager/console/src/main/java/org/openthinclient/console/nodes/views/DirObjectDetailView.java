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

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import org.openide.nodes.Node;
import org.openide.windows.TopComponent;
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
import org.openthinclient.console.ui.CollapsibleTitlePanel;
import org.openthinclient.console.util.DetailViewFormBuilder;
import org.openthinclient.ldap.DirectoryException;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.levigo.util.swing.IconManager;

/**
 * @author Natalie Bohnert
 */
public class DirObjectDetailView extends AbstractDetailView {

	private DirectoryObject dirObject;
	private Realm realm;

	@Override
	public JComponent getHeaderComponent() {
		final DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
				"p, 10dlu, r:p, 3dlu, f:p:g"), Messages.getBundle()); //$NON-NLS-1$
		dfb.setLeadingColumnOffset(2);
		dfb.setColumn(3);

		final String simpleClassName = dirObject.getClass().getSimpleName();
		Font f = UIManager.getFont("TitledBorder.font"); //$NON-NLS-1$
		f = f.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));

		final JLabel typeLabel = new JLabel(Messages.getString("types.singular." //$NON-NLS-1$
				+ simpleClassName) + ":"); //$NON-NLS-1$
		typeLabel.setForeground(new Color(10, 10, 150));
		typeLabel.setFont(f);
		JLabel nameLabel = new JLabel();
		if (simpleClassName.equals("Realm")) {
			String dn = realm.getConnectionDescriptor().getBaseDN();
			dn = dn.replace("\\,", "#%COMMA%#");
			final String[] s = dn.split(",");
			String nameRealm = "";
			if (s.length > 0) {
				nameRealm = s[0].replace("ou=", "").trim();
				nameRealm = nameRealm.replace("#%COMMA%#", "\\,").trim();
			}
			nameLabel = new JLabel(nameRealm); //$NON-NLS-1$
		} else
			nameLabel = new JLabel(dirObject.getName() != null
					? dirObject.getName()
					: ""); //$NON-NLS-1$
		nameLabel.setForeground(new Color(50, 50, 200));
		nameLabel.setFont(f);

		final JLabel descField = new JLabel(dirObject.getDescription() != null
				? dirObject.getDescription()
				: ""); //$NON-NLS-1$

		dfb.append(typeLabel, nameLabel);
		dfb.append(descField, 3);

		dfb.add(new JLabel(IconManager.getInstance(DetailViewProvider.class,
				"icons").getIcon("tree." + dirObject.getClass().getSimpleName())), //$NON-NLS-1$ //$NON-NLS-2$
				new CellConstraints(1, 1, 1, dfb.getRowCount(), CellConstraints.CENTER,
						CellConstraints.TOP));

		return dfb.getPanel();
	}

	/*
	 * @see org.openthinclient.console.DetailView#getMainComponent()
	 */
	public JComponent getMainComponent() {
		final List<JComponent> sections = new LinkedList<JComponent>();
		if (dirObject instanceof User)
			sections.add(new UserView((User) dirObject));

		if (dirObject instanceof Client)
			sections.add(new ClientView((Client) dirObject));

		if (dirObject instanceof UnrecognizedClient)
			sections.add(new UnrecognizedClientView((UnrecognizedClient) dirObject));

		if (dirObject instanceof Profile)
			sections.add(new ProfileView((Profile) dirObject, realm));

		if (dirObject instanceof Group)
			sections.add(new IncomingAssociationsView((Group) dirObject));

		// add "administrators" section
		if (dirObject instanceof Realm) {
			final IncomingAssociationsView view = new IncomingAssociationsView(
					(Group) ((Realm) dirObject).getAdministrators());
			view.setName(Messages.getString("DirObjectDetailView.administrators")); //$NON-NLS-1$
			sections.add(view);
		}

		if (dirObject instanceof AssociatedObjectsProvider)
			sections.add(new OutgoingAssociationsView(
					(AssociatedObjectsProvider) dirObject));

		final DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
				"f:p:g")); //$NON-NLS-1$
		for (final JComponent component : sections)
			dfb
					.append(new CollapsibleTitlePanel(component.getName(), component,
							true));

		final JScrollPane scrollPane = new JScrollPane(dfb.getPanel());
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.setPreferredSize(scrollPane.getMinimumSize());
		scrollPane.setBorder(null);

		return scrollPane;
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
			} else if (node instanceof RealmNode) {
				dirObject = realm = (Realm) node.getLookup().lookup(Realm.class);
				try {
					realm.ensureInitialized();
				} catch (final DirectoryException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		if (null == dirObject)
			throw new IllegalStateException(
					"Could not lookup a DirectoryObject instance"); //$NON-NLS-1$ 

		if (null == realm)
			throw new IllegalStateException("Could not lookup a Realm instance"); //$NON-NLS-1$
	}

	/*
	 * @see org.openthinclient.console.SubDetailView#getTitle()
	 */
	public String getTitle() {
		return null;
	}

}
