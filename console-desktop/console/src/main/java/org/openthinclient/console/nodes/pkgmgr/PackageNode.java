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

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.levigo.util.swing.IconManager;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;
import org.openthinclient.console.*;
import org.openthinclient.console.nodes.MyAbstractNode;
import org.openthinclient.console.ui.CollapsibleTitlePanel;
import org.openthinclient.console.util.DetailViewFormBuilder;
import org.openthinclient.pkgmgr.db.Package;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** Getting the feed node and wrapping it in a FilterNode */
public class PackageNode extends MyAbstractNode
		implements
			DetailViewProvider,
			EditorProvider,
			Refreshable {

	/**
	 * @param node
	 * @param keys
	 * @throws IntrospectionException
	 */
	public PackageNode(Node node, Package pkg) {
		super(Children.LEAF, new ProxyLookup(new Lookup[] { Lookups.fixed(new Object[] { pkg }), node.getLookup() }));
	}

	@Override
	public String getName() {
		return ((Package) getLookup().lookup(Package.class)).getName();
	}

	@Override
	public Action[] getActions(boolean context) {
		// no action if not writable
		if (!isWritable())
			return new Action[] {};

		// actions by node
		final Node parentNode = getParentNode();
		if (parentNode instanceof InstalledPackagesNode)
			return new Action[] { SystemAction.get(DeleteAction.class) };
		else if (parentNode instanceof AvailablePackagesNode)
			return new Action[] { SystemAction.get(InstallAction.class) };
		else if (parentNode instanceof UpdatablePackagesNode)
			return new Action[] { SystemAction.get(UpdateAction.class) };
		else if (parentNode instanceof AlreadyDeletedPackagesNode)
			return new Action[] { SystemAction.get(RealyDeleteAction.class) };
		else if (parentNode instanceof DebianFilePackagesNode)
			return new Action[] { SystemAction.get(DebianPackagesDeleteAction.class) };

		return new Action[] {};
	}

	@Override
	public SystemAction getDefaultAction() {
		if (isWritable())
			return SystemAction.get(PackageListNodeActionForPackageNode.class);

		return null;
	}

	/*
	 * @see org.openide.nodes.FilterNode#canCopy()
	 */
	@Override
	public boolean canCopy() {
		return true;
	}

	/*
	 * @see org.openide.nodes.FilterNode#canDestroy()
	 */
	@Override
	public boolean canDestroy() {
		return false;
	}

	/*
	 * @see org.openide.nodes.FilterNode#canRename()
	 */
	@Override
	public boolean canRename() {
		return false;
	}

	/*
	 * @see org.openthinclient.console.DetailViewProvider#getDetailView()
	 */
	public DetailView getDetailView() {
		return new PackageDetailView();

	}

	// /*
	// * @see org.openthinclient.console.nodes.MyAbstractNode#getIcon(int)
	// */
	// @Override
	// public Image getIcon(int type) {
	// DirectoryObject o = (DirectoryObject) getLookup().lookup(
	// DirectoryObject.class);
	// return IconManager.getInstance(DetailViewProvider.class, "icons").getImage(
	// //$NON-NLS-1$
	// "tree." + o.getClass().getSimpleName()); //$NON-NLS-1$
	// }

	/*
	 * @see org.openthinclient.console.EditorProvider#getEditor()
	 */
	public DetailView getEditor() {
		return null;
	}

	/*
	 * @see org.openthinclient.console.Refreshable#refresh()
	 */
	public void refresh() {
		// FIXME
	}

	// public Package getPackage(){
	// return()
	// }

	public void refresh(String type) {
		// FIXME!

	}

	@Override
	public Image getIcon(int type) {
		return getOpenedIcon(type);
	}

	@Override
	public Image getOpenedIcon(int type) {
		return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
				"tree." + getClass().getSimpleName()); //$NON-NLS-1$
	}

	public static final class PackageDetailView implements DetailView {
		private Package p;
		private Node node2;
		private PackageManagerDelegation pkgmgr;

		public JComponent getFooterComponent() {
			return null;
		}

		public JComponent getHeaderComponent() {
			if (null == p)
				return null;

			final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
					new FormLayout("p, 10dlu, r:p, 3dlu, f:p:g"), Messages.getBundle()); //$NON-NLS-1$
			dfb.setLeadingColumnOffset(2);
			dfb.setColumn(3);

			final String simpleClassName = p.getClass().getSimpleName();
			Font f = UIManager.getFont("TitledBorder.font"); //$NON-NLS-1$
			f = f.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));

			final JLabel typeLabel = new JLabel(Messages.getString("types.singular." //$NON-NLS-1$
					+ simpleClassName) + ":"); //$NON-NLS-1$
			typeLabel.setForeground(new Color(10, 10, 150));
			typeLabel.setFont(f);

			final JLabel nameLabel = new JLabel(p.getName() != null
					? p.getName()
					: ""); //$NON-NLS-1$
			nameLabel.setForeground(new Color(50, 50, 200));
			nameLabel.setFont(f);
			dfb.append(typeLabel, nameLabel);
			dfb.add(
					new JLabel(
							IconManager
									.getInstance(DetailViewProvider.class, "icons").getIcon("tree." + p.getClass().getSimpleName())), //$NON-NLS-1$ //$NON-NLS-2$
					new CellConstraints(1, 1, 1, dfb.getRowCount(),
							CellConstraints.CENTER, CellConstraints.TOP));

			return dfb.getPanel();
		}

		public JComponent getMainComponent() {
			if (null == p)
				return null;

			final String simpleClassName = "PackageData";
			Font f = UIManager.getFont("TitledBorder.font"); //$NON-NLS-1$
			f = f.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));

			final JLabel typeLabel = new JLabel(Messages.getString("types.singular." //$NON-NLS-1$
					+ simpleClassName) + ":"); //$NON-NLS-1$
			typeLabel.setForeground(new Color(10, 10, 150));
			typeLabel.setFont(f);

			final JLabel generalLabel = new JLabel(p.getName() != null
					? p.getName()
					: ""); //$NON-NLS-1$
			generalLabel.setName(Messages.getString("types.singular." //$NON-NLS-1$
					+ simpleClassName) + ":");
			generalLabel.setForeground(new Color(50, 50, 200));
			generalLabel.setFont(f);

			final List<JComponent> sections = new LinkedList<JComponent>();

			sections.add(createDescriptionPanel());
			sections.add(createBasePanel());
			sections.add(createInstallPanel());
			sections.add(createDependencyPanel());
			sections.add(createChangelogPanel());
			final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
					new FormLayout("f:p:g", "f:p"));
			for (final JComponent component : sections)
				dfb.append(new CollapsibleTitlePanel(component.getName(), component,
						true));

			final JScrollPane scrollPane = new JScrollPane(dfb.getPanel());
			scrollPane.getVerticalScrollBar().setUnitIncrement(20);
			scrollPane.setPreferredSize(scrollPane.getMinimumSize());
			scrollPane.setBorder(null);

			return scrollPane;
		}

		private JComponent createDependencyPanel() {
			final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
					new FormLayout("f:p:g", "f:p:g"));
			final List<JComponent> sections = new LinkedList<JComponent>();
			sections.add(dependencyHandling(
					Messages.getString("node.PackageNode.PackageDetailView.Depends")
							+ ":", p.getDepends().toString()));
			sections.add(dependencyHandling(
					Messages.getString("node.PackageNode.PackageDetailView.Pre-Depends")
							+ ":", p.getPreDepends().toString()));
			sections.add(dependencyHandling(
					Messages.getString("node.PackageNode.PackageDetailView.Conflicts")
							+ ":", p.getConflicts().toString()));
			sections.add(dependencyHandling(
					Messages.getString("node.PackageNode.PackageDetailView.Provides")
							+ ":", p.getProvides().toString()));
			for (final JComponent component : sections)
				dfb.append(new CollapsibleTitlePanel(component.getName(), component,
						false));

			dfb.getPanel()
					.setName(
							Messages
									.getString("node.PackageNode.PackageDetailView.createDependencyPanel.PanelName"));

			return dfb.getPanel();
		}

		private JComponent dependencyHandling(String name, String depends) {
			final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
					new FormLayout("left:p:g"));
			while (depends.length() > 0)
				if (depends.contains(",")) {
					dfb.append(depends.substring(0, depends.indexOf(",")).trim());
					depends = depends.substring(depends.indexOf(",") + 1);
				} else {
					dfb.append(depends);
					depends = "";
				}
			dfb.getPanel().setName(name);
			return dfb.getPanel();

		}

		private JComponent createDescriptionPanel() {
			final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
					new FormLayout("f:p:g:"));
			String descript = p.getDescription();

			if (descript == null) {
				dfb.append(new JLabel(
						Messages
								.getString("node.PackageNode.PackageDetailView.createDescriptionPanel.noDescriptionAvailable")));
				dfb.getPanel()
						.setName(
								Messages
										.getString("node.PackageNode.PackageDetailView.createDescriptionPanel.PanelName"));

				return dfb.getPanel();
			}

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
			dfb.getPanel()
					.setName(
							Messages
									.getString("node.PackageNode.PackageDetailView.createDescriptionPanel.PanelName"));

			return dfb.getPanel();
		}

		private JComponent createInstallPanel() {
			final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
					new FormLayout("p,2dlu,left:p"));
			final float groesse = p.getSize();

			float calcSize = groesse / 1024f / 1024f;

			calcSize *= 10000f;
			calcSize = Math.round(calcSize) / 10000f;

			dfb.append(Messages.getString("node.PackageNode.PackageDetailView.Size")
					+ ":", new JLabel(String.valueOf(calcSize) + " MB"));
			dfb.append(
					Messages.getString("node.PackageNode.PackageDetailView.Filename")
							+ ":",
					new JLabel(p.getFilename().substring(
							p.getFilename().lastIndexOf("/") + 1)));
			dfb.getPanel()
					.setName(
							Messages
									.getString("node.PackageNode.PackageDetailView.createInstallPanel.PanelName"));

			return dfb.getPanel();
		}

		private JComponent createBasePanel() {
			final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
					new FormLayout("l:p,2dlu,f:p:g"));
			String temp;
			if (p.getVersion().toString().startsWith("0:"))
				temp = p.getVersion().toString()
						.substring(2, p.getVersion().toString().length());
			else
				temp = p.getVersion().toString();
			dfb.append(Messages.getString("node.PackageNode.PackageDetailView.Name")
					+ ":", new JLabel(p.getName()));
			dfb.append(
					Messages.getString("node.PackageNode.PackageDetailView.Version")
							+ ":", new JLabel(temp));
			dfb.append(
					Messages.getString("node.PackageNode.PackageDetailView.Section")
							+ ":", new JLabel(p.getSection()));
			dfb.append(
					Messages.getString("node.PackageNode.PackageDetailView.Priority")
							+ ":", new JLabel(p.getPriority()));
			dfb.getPanel()
					.setName(
							Messages
									.getString("node.PackageNode.PackageDetailView.createBasePanel.PanelName"));
			return dfb.getPanel();
		}

		private JComponent createChangelogPanel() {
			if (node2.getParentNode().getName()
					.equalsIgnoreCase(Messages.getString("node.UpdatablePackagesNode")))
				;
			final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
					new FormLayout("f:p:g"));
			List<String> changeLog = new ArrayList<String>();
			if (node2.getParentNode().getName()
					.equalsIgnoreCase(Messages.getString("node.DebianFilePackagesNode"))
					|| node2
							.getParentNode()
							.getName()
							.equalsIgnoreCase(
									Messages.getString("node.AlreadyDeletedPackagesNode"))
					|| null == pkgmgr.getChangelogFile(p))
				changeLog
						.add(Messages
								.getString("node.PackageNode.PackageDetailView.createChangelogPanel.noChangeLogFile"));
			else
				changeLog = new ArrayList<String>(pkgmgr.getChangelogFile(p));

			List<JComponent> sections = new LinkedList<JComponent>();
			if (node2.getParentNode().getName()
					.equalsIgnoreCase(Messages.getString("node.UpdatablePackagesNode")))
				sections = createChangelogListOnlyNewer(changeLog);
			else
				sections = createChangelogList(changeLog);
			for (final JComponent component : sections)
				dfb.append(new CollapsibleTitlePanel(component.getName(), component,
						false));
			dfb.getPanel()
					.setName(
							Messages
									.getString("node.PackageNode.PackageDetailView.createChangelogPanel.PanelName"));

			return dfb.getPanel();

		}

		private List<JComponent> createChangelogList(List<String> changeLog) {
			final List<JComponent> sections = new LinkedList<JComponent>();
			final ArrayList<String> lines = new ArrayList<String>();
			for (final String line : changeLog)
				if (line.length() != 0)
					if (line.startsWith(p.getName())) {
						if (lines.size() > 0) {
							sections.add(createChangeLogEntry(lines));
							lines.clear();
						}

						lines.add(line);
					} else
						lines.add(line);
			if (lines.size() > 0)
				sections.add(createChangeLogEntry(lines));
			return sections;
		}

		private List<JComponent> createChangelogListOnlyNewer(List<String> changeLog) {
			final List<JComponent> sections = new LinkedList<JComponent>();
			final ArrayList<String> lines = new ArrayList<String>();
			Package oldPack = null;
			for (final Package pack : pkgmgr.getInstalledPackages())
				if (pack.getName().equalsIgnoreCase(p.getName())) {
					oldPack = pack;
					break;
				}

			final String version = oldPack.getVersion().toString()
					.substring(oldPack.getVersion().toString().indexOf(":") + 1);
			boolean contains = false;
			for (final String line : changeLog) {
				if (line.contains(version))
					contains = true;
				if (line.length() != 0 && !contains)
					if (line.startsWith(oldPack.getName())) {
						if (lines.size() > 0) {
							sections.add(createChangeLogEntry(lines));
							lines.clear();
						}

						lines.add(line);
					} else
						lines.add(line);
			}
			if (lines.size() > 0)
				sections.add(createChangeLogEntry(lines));
			return sections;
		}

		private JComponent createChangeLogEntry(ArrayList<String> lines) {
			final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
					new FormLayout("left:p"));
			final String firstLine = lines.get(0);

			lines.remove(0);
			for (final String line : lines)
				dfb.append(new JLabel(line));
			dfb.getPanel().setName(firstLine);
			return dfb.getPanel();
		}

		public void init(Node[] selection, TopComponent tc) {
			for (final Node node : selection) {
				this.node2 = node;
				p = (Package) node.getLookup().lookup(Package.class);
				pkgmgr = ((PackageManagementNode) node.getParentNode().getParentNode())
						.getPackageManagerDelegation();
			}

		}

	}
}
