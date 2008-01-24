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
package org.openthinclient.console.nodes.pkgmgr;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openthinclient.console.Messages;
import org.openthinclient.console.ui.CollapsibleTitlePanel;
import org.openthinclient.console.util.DetailViewFormBuilder;
import org.openthinclient.util.dpkg.Package;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ModifyDialog extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4900661111130191583L;
	int ret = 0;
	String action = "";

	public int shouldPackagesBeUsed(Collection<Package> packages, String name) {
		final CellConstraints cc = new CellConstraints();
		this.setLayout(new FormLayout("f:p:g", "f:p:g,f:p:g"));
		final DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
				"f:p:g", "p,f:p:g"));
		final List<JComponent> sections = new LinkedList<JComponent>();
		for (final Package pkg : packages)
			sections.add(createPackageInfos(pkg));
		final JComponent headerComponent = createNameLabel(name);
		headerComponent.setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createMatteBorder(0, 0, 1, 0, getBackground().darker()),
				headerComponent.getBorder()));
		dfb.append(headerComponent);
		dfb.append(makeScrollPain(sections));
		add(dfb.getPanel(), cc.xy(1, 1));
		final JButton backButton = new JButton(Messages.getString("back"));
		backButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ret = -1;
			}
		});
		final JButton cancelButton = new JButton(Messages.getString("Cancel"));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ret = 0;
			}
		});
		final JButton okButton = new JButton(Messages.getString("OK"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ret = 1;
			}
		});
		final JLabel label = new JLabel(Messages.getString("ModifyDialog.question",
				action));
		setPreferredSize(new Dimension(800, 600));
		setVisible(true);
		// DIALOG Descriptor
		final DialogDescriptor descriptor = new DialogDescriptor(this, name, true,
				new Object[]{label, backButton, cancelButton, okButton}, okButton,
				DialogDescriptor.BOTTOM_ALIGN, null, new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					}
				});
		descriptor.setClosingOptions(new Object[]{cancelButton, okButton,
				backButton});
		// DIALOG
		final Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
		dialog.setVisible(true);
		return ret;
	}

	public JComponent createPackageInfos(Package pkg) {
		final DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
				"p"));
		String versTemp;
		if (pkg.getVersion().toString().startsWith("0:"))
			versTemp = pkg.getVersion().toString().substring(2,
					pkg.getVersion().toString().length());
		else
			versTemp = pkg.getVersion().toString();
		dfb.append(new JLabel(Messages
				.getString("node.PackageNode.PackageDetailView.Version")
				+ ": " + versTemp));
		dfb.append(new JLabel(Messages
				.getString("node.PackageNode.PackageDetailView.Section")
				+ " : " + pkg.getSection()));
		dfb.append(new JLabel(Messages
				.getString("node.PackageNode.PackageDetailView.Priority")
				+ " : " + pkg.getPriority()));

		if (pkg.getDescription() == null) {
			dfb
					.append(new JLabel(
							Messages
									.getString("node.PackageNode.PackageDetailView.createDescriptionPanel.noDescriptionAvailable")));
			dfb.getPanel().setName(pkg.getName());
			return dfb.getPanel();
		}
		String descript = pkg.getDescription();
		while (descript.length() > 80) {
			descript = descript.trim();
			final String temp = descript.substring(0, 80);
			if (temp.contains("\n")) {
				dfb.append(new JLabel(temp.substring(0, temp.indexOf("\n"))));
				dfb.append(new JLabel(""));
				descript = descript.substring(temp.indexOf("\n") + 2);
			} else {
				dfb.append(new JLabel(descript.substring(0, temp.lastIndexOf(" "))));
				descript = descript.substring(temp.lastIndexOf(" "));
			}
		}
		descript = descript.trim();
		while (descript.contains("\n")) {
			descript = descript.trim();
			dfb.append(new JLabel(descript.substring(0, descript.indexOf("\n"))));
			dfb.append(new JLabel(""));
			descript = descript.substring(descript.indexOf("\n") + 2);
		}
		descript = descript.trim();
		dfb.append(new JLabel(descript));
		dfb.getPanel().setName(pkg.getName());
		return dfb.getPanel();
	}

	public JComponent makeScrollPain(List<JComponent> sections) {
		final DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
				"f:p:g", "p"));

		for (final JComponent component : sections)
			dfb
					.append(new CollapsibleTitlePanel(component.getName(), component,
							true));

		final JScrollPane scrollPane = new JScrollPane(dfb.getPanel());
		scrollPane.getVerticalScrollBar().setUnitIncrement(50);
		scrollPane.setPreferredSize(new Dimension(800, 550));
		scrollPane.setBorder(null);
		return scrollPane;
	}

	public JComponent createNameLabel(String name) {

		Font f = UIManager.getFont("TitledBorder.font"); //$NON-NLS-1$
		f = f.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));
		String nameAction = "";
		if (name.equalsIgnoreCase(Messages.getString("node.AvailablePackagesNode"))) {
			nameAction = Messages
					.getString("ModifyDialog.createNameLabel.AvailablePackagesNode");
			action = Messages.getString("ModifyDialog.action.install");
		}

		else if (name.equalsIgnoreCase(Messages
				.getString("node.InstalledPackagesNode"))) {
			nameAction = Messages
					.getString("ModifyDialog.createNameLabel.InstalledPackagesNode");
			action = Messages.getString("ModifyDialog.action.delete");

		}

		else if (name.equalsIgnoreCase(Messages
				.getString("node.UpdatablePackagesNode"))) {
			nameAction = Messages
					.getString("ModifyDialog.createNameLabel.UpdatablePackagesNode");
			action = Messages.getString("ModifyDialog.action.update");

		}

		else if (name.equalsIgnoreCase(Messages
				.getString("node.AlreadyDeletedPackagesNode"))) {
			nameAction = Messages
					.getString("ModifyDialog.createNameLabel.AlreadyDeletedPackagesNode");
			action = Messages.getString("ModifyDialog.action.delete");

		} else if (name.equalsIgnoreCase(Messages
				.getString("node.DebianFilePackagesNode"))) {
			nameAction = Messages
					.getString("ModifyDialog.createNameLabel.deleteDebianPackagesNode");
			action = Messages.getString("ModifyDialog.action.debianDelete");

		}

		else
			nameAction = Messages.getString("node.unknown");
		final DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
				"f:p:g"), Messages.getBundle());
		final JLabel nameLabel = new JLabel(nameAction); //$NON-NLS-1$
		nameLabel.setForeground(new Color(50, 50, 200));
		nameLabel.setFont(f);
		dfb.append(nameLabel);
		return dfb.getPanel();
	}

}
