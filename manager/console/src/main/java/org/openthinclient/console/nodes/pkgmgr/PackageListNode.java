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

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.ProxyLookup;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.DetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.Messages;
import org.openthinclient.console.Refreshable;
import org.openthinclient.console.nodes.AbstractAsyncArrayChildren;
import org.openthinclient.console.nodes.ErrorNode;
import org.openthinclient.console.nodes.MyAbstractNode;
import org.openthinclient.util.dpkg.Package;

import com.levigo.util.swing.IconManager;

/** Getting the feed node and wrapping it in a FilterNode */
public abstract class PackageListNode extends MyAbstractNode
		implements
			DetailViewProvider,
			Refreshable {
	private PackageManagerDelegation pkgmgr;

	private class Children extends AbstractAsyncArrayChildren {
		private PackageManagerDelegation pkgmgr;

		@Override
		@SuppressWarnings("unchecked")
		protected Collection<Package> asyncInitChildren() {
			try {
				if (null == pkgmgr) {
					final Realm realm = (Realm) getNode().getLookup().lookup(Realm.class);
					pkgmgr = realm.getPackageManagerDelegation();
				}

				List<Package> sorted;
				if (getPackageList(pkgmgr).size() > 0)
					sorted = new ArrayList<Package>(getPackageList(pkgmgr));
				else
					sorted = Collections.EMPTY_LIST;

				Collections.sort(sorted);
				return sorted;
			} catch (final Exception e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
				add(new Node[]{new ErrorNode(Messages
						.getString("DirObjectListNode.cantDisplay"), e)}); //$NON-NLS-1$
				return Collections.EMPTY_LIST;
			}
		}

		@Override
		protected Node[] createNodes(Object key) {
			return new Node[]{new PackageNode(getNode(), (Package) key)};
		}
	}

	/**
	 * @param parent
	 * @param keys
	 */
	public PackageListNode(Node parent) {
		super(Children.LEAF, new ProxyLookup(new Lookup[]{parent.getLookup()}));

		setChildren(new Children());
	}

	/**
	 * 
	 * @param pkgmgr
	 * @return return a list of packages which are depend on the Class which
	 *         extends this
	 */
	protected abstract Collection<Package> getPackageList(
			PackageManagerDelegation pkgmgr);

	/*
	 * @see org.openide.nodes.FilterNode#canCopy()
	 */
	@Override
	public boolean canCopy() {
		return false;
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

	// /*
	// * @see org.openthinclient.console.nodes.MyAbstractNode#getIcon(int)
	// */
	// @Override
	// public Image getIcon(int type) {
	// Class typeClass = (Class) getLookup().lookup(Class.class);
	// System.out.println(IconManager.getInstance(DetailViewProvider.class,
	// "icons").getImage(
	// "tree.list." + typeClass.getSimpleName(),
	// IconManager.EFFECT_MORECONTRAST).toString());
	// return IconManager.getInstance(DetailViewProvider.class,
	// "icons").getImage(
	// "tree.list." + typeClass.getSimpleName(),
	// IconManager.EFFECT_MORECONTRAST);
	// }
	/*
	 * @see org.openide.nodes.FilterNode#getIcon(int)
	 */
	@Override
	public Image getIcon(int type) {
		return getOpenedIcon(type);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openthinclient.console.nodes.MyAbstractNode#getOpenedIcon(int)
	 */
	@Override
	public Image getOpenedIcon(int type) {
		return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
				"tree." + getClass().getSimpleName()); //$NON-NLS-1$
	}

	/*
	 * @see org.openthinclient.console.DetailViewProvider#getDetailView()
	 */
	public DetailView getDetailView() {
		return PackageDetailView.getInstance();
	}

	/*
	 * @see org.openthinclient.console.Refreshable#refresh()
	 */
	public void refresh() {
		((AbstractAsyncArrayChildren) getChildren()).refreshChildren();
	}

	/**
	 * @param context
	 * @param actions
	 * @return A array of actions which could be associated with the extending
	 *         class
	 */
	protected Action[] getActions(boolean context, Action... actions) {
		final Action superActions[] = new Action[]{SystemAction
				.get(RefreshPackageManagerValuesAction.class)};
		Action result[];

		if (null == pkgmgr) {
			final Realm realm = (Realm) getLookup().lookup(Realm.class);
			pkgmgr = realm.getPackageManagerDelegation();
		}

		if (getPackageList(pkgmgr).size() > 0) {
			final Action allActions[] = new Action[]{SystemAction
					.get(PackageListNodeAction.class)};
			result = new Action[superActions.length + actions.length
					+ allActions.length];
			System.arraycopy(superActions, 0, result, 0, superActions.length);
			System.arraycopy(actions, 0, result, superActions.length, actions.length);
			System.arraycopy(allActions, 0, result,
					(superActions.length + actions.length), allActions.length);
		} else {
			result = new Action[superActions.length + actions.length];
			System.arraycopy(superActions, 0, result, 0, superActions.length);
			System.arraycopy(actions, 0, result, superActions.length, actions.length);
		}
		return result;
	}

}
