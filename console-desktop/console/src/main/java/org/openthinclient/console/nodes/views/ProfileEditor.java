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
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.openide.ErrorManager;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.ChoiceNode;
import org.openthinclient.common.model.schema.EntryNode;
import org.openthinclient.common.model.schema.GroupNode;
import org.openthinclient.common.model.schema.Node;
import org.openthinclient.common.model.schema.PasswordNode;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.SectionNode;
import org.openthinclient.common.model.schema.ValueNode;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.console.Messages;
import org.openthinclient.console.ui.SectionPanel;
import org.openthinclient.console.ui.UIFactory;
import org.openthinclient.console.ui.fields.ChoiceField;
import org.openthinclient.console.ui.fields.EntryField;
import org.openthinclient.console.ui.fields.PasswordField;
import org.openthinclient.console.util.DetailViewFormBuilder;

import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Natalie Bohnert
 */
public class ProfileEditor extends JPanel {
	private static final Logger logger = Logger.getLogger(ProfileEditor.class);

	public ProfileEditor(Profile profile, Realm realm) {
		if (profile != null)
			try {
				final Schema schema = profile.getSchema(realm);
				Node invisibleNode = null;
				if (profile.getClass() == Realm.class) {
					invisibleNode = schema.getChild("invisibleObjects");
					schema.removeChild(invisibleNode);
				}

				if (schema != null)
					layoutConfigPanel(this, profile, schema, 0, null);

				if (invisibleNode != null)
					schema.addChild(invisibleNode);
			} catch (final SchemaLoadingException e) {
				ErrorManager
						.getDefault()
						.annotate(
								e,
								ErrorManager.EXCEPTION,
								Messages.getString("ProfileEditor.couldntLoadSchema"), null, null, null); //$NON-NLS-1$
				ErrorManager.getDefault().notify(e);
			}

		setName(Messages.getString("Profile_title")); //$NON-NLS-1$
	}

	/**
	 * The name says it all.
	 * 
	 * @param builder
	 */
	private void layoutValue(Profile profile, Node node,
			DetailViewFormBuilder builder) {
		final String warning = profile.getWarning(node.getKey());
		if (warning != null) {
			final JLabel warnLabel = new JLabel(Messages.getString(warning));
			warnLabel.setForeground(Color.RED);
			builder.append(warnLabel, builder.getColumnCount()
					- builder.getLeadingColumnOffset());
			builder.nextLine();
		}

		String label = node.getLabel();
		if (null == label)
			label = ""; //$NON-NLS-1$

		if (node instanceof ChoiceNode)
			builder.append(label, new ChoiceField(profile, (ChoiceNode) node)
					.getEditor());
		else if (node instanceof PasswordNode)
			builder.append(label, new PasswordField(profile, (PasswordNode) node)
					.getEditor());
		else if (node instanceof EntryNode)
			builder.append(label, new EntryField(profile, (EntryNode) node)
					.getEditor());
		else if (node instanceof ValueNode)
			; // we ignore those
		else
			logger.warn(Messages.getString("ProfileEditor.unknownType", node
					.getClass()));
	}

	/**
	 * @param profile
	 * @param node
	 * @param title TODO
	 */
	private void layoutConfigPanel(JPanel panel, Profile profile, Node node,
			int indent, String title) {
		final DetailViewFormBuilder builder = new DetailViewFormBuilder(
				new FormLayout(
						indent + "dlu, left:default, 6dlu, fill:default:grow", ""), Messages //$NON-NLS-1$ //$NON-NLS-2$
						.getBundle(), panel);
		builder.setLeadingColumnOffset(1);
		builder.nextColumn();

		if (null != title) {
			builder.appendTitle(title);
			builder.nextLine();
		}

		layoutNode(profile, node, builder);

		if (UIFactory.DEBUG)
			panel.setBorder(BorderFactory.createEtchedBorder());
	}

	/**
	 * The name says it all.
	 * 
	 * @param profile
	 * @param node
	 * @param builder
	 */
	private void layoutNode(Profile profile, Node node,
			DetailViewFormBuilder builder) {
		for (final Iterator i = node.getChildren().iterator(); i.hasNext();) {
			final Node child = (Node) i.next();
			if (child instanceof EntryNode)
				layoutValue(profile, child, builder);
			else if (child instanceof GroupNode)
				layoutGroup(profile, builder, (GroupNode) child);
			else if (child instanceof SectionNode)
				layoutSection(profile, builder, (SectionNode) child);
		}
	}

	/**
	 * @param profile
	 * @param builder
	 * @param child
	 */
	private void layoutGroup(Profile profile, DetailViewFormBuilder builder,
			GroupNode child) {
		if (child.getParent() instanceof GroupNode) {
			// nested groups are indented
			final JPanel groupPanel = new JPanel();
			layoutConfigPanel(groupPanel, profile, child, 10, child.getLabel());
			groupPanel.setBorder(null);

			builder.append(groupPanel, builder.getColumnCount()
					- builder.getLeadingColumnOffset());
		} else if (child.getLabel() == null) {
			builder.appendUnrelatedComponentsGapRow();
			builder.nextRow();
			layoutNode(profile, child, builder);
			builder.appendUnrelatedComponentsGapRow();
			builder.nextRow();
		} else {
			builder.appendSeparator(child.getLabel());
			layoutNode(profile, child, builder);
		}
	}

	/**
	 * @param profile
	 * @param builder
	 * @param section
	 */
	private void layoutSection(Profile profile, DetailViewFormBuilder builder,
			SectionNode section) {
		final JPanel sectionContents = new JPanel();
		layoutConfigPanel(sectionContents, profile, section, 0, null);
		builder.append(new SectionPanel(section.getLabel(), sectionContents,
				section.isCollapsed()), builder.getColumnCount()
				- builder.getLeadingColumnOffset());
		builder.nextLine();
	}
}
