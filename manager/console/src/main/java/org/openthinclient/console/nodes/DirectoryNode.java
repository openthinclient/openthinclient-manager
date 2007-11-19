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
package org.openthinclient.console.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.swing.Action;

import org.openide.ErrorManager;
import org.openide.actions.DeleteAction;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openthinclient.console.DetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.EditAction;
import org.openthinclient.console.Messages;
import org.openthinclient.console.nodes.views.DirectoryEntryDetailView;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

import com.levigo.util.swing.IconManager;

public class DirectoryNode extends FilterNode implements DetailViewProvider {
	static class Children extends AbstractAsyncArrayChildren {
		@Override
		protected Collection asyncInitChildren() {
			try {
				final LDAPConnectionDescriptor lcd = ((DirectoryNode) getNode())
						.getConnectionDescriptor();

				final List<String> partitions = new ArrayList<String>();
				final DirContext ctx = lcd.createDirContext();
				try {
					final Attributes a = ctx.getAttributes(
							"", new String[]{"namingContexts"}); //$NON-NLS-1$ //$NON-NLS-2$
					final Attribute namingContexts = a.get("namingContexts"); //$NON-NLS-1$
					if (null == namingContexts)
						throw new NamingException(Messages
								.getString("DirectoryNode.noPartitionList")); //$NON-NLS-1$

					final NamingEnumeration<?> allAttributes = namingContexts.getAll();
					while (allAttributes.hasMore())
						partitions.add(allAttributes.next().toString());
				} finally {
					if (null != ctx)
						ctx.close();
				}

				return partitions;
			} catch (final Exception e) {
				ErrorManager.getDefault().notify(e);
				removeAllChildren();
				add(new Node[]{new ErrorNode(Messages
						.getString("DirectoryNode.cantDisplay"), e)}); //$NON-NLS-1$

				return Collections.EMPTY_LIST;
			}
		}

		@Override
		protected Node[] createNodes(Object key) {
			final LDAPConnectionDescriptor lcd = ((DirectoryNode) getNode())
					.getConnectionDescriptor();

			return new Node[]{((DirectoryNode) getNode()).createChild(lcd,
					(String) key)};
		}

	}

	public DirectoryNode(Node dataNode, Node parent, LDAPConnectionDescriptor cd) {
		super(dataNode, new Children(), new ProxyLookup(new Lookup[]{
				Lookups.fixed(new Object[]{cd}), parent.getLookup()}));

		disableDelegation(DELEGATE_GET_DISPLAY_NAME);
	}

	/**
	 * @param connectionDescriptor
	 */
	public DirectoryNode(LDAPConnectionDescriptor connectionDescriptor) {
		this(Node.EMPTY, Node.EMPTY, connectionDescriptor);
	}

	/**
	 * @param lcd
	 * @param rdn
	 * @return
	 */
	protected PartitionNode createChild(LDAPConnectionDescriptor lcd, String rdn) {
		return new PartitionNode(this, lcd, rdn);
	}

	@Override
	public String getName() {
		final LDAPConnectionDescriptor cd = (LDAPConnectionDescriptor) getLookup()
				.lookup(LDAPConnectionDescriptor.class);
		return cd.getLDAPUrl();
	}

	/*
	 * @see org.openide.nodes.FilterNode#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return ((LDAPConnectionDescriptor) getLookup().lookup(
				LDAPConnectionDescriptor.class)).getLDAPUrl();
	}

	@Override
	public Action[] getActions(boolean context) {
		return new Action[]{SystemAction.get(DeleteAction.class)};
	}

	/*
	 * @see org.openthinclient.console.DetailViewProvider#getDetailView()
	 */
	public DetailView getDetailView() {
		return new DirectoryEntryDetailView();
	}

	/*
	 * @see org.openide.nodes.FilterNode#getIcon(int)
	 */
	@Override
	public Image getIcon(int type) {
		return getOpenedIcon(type);
	}

	/*
	 * @see org.openide.nodes.FilterNode#getOpenedIcon(int)
	 */
	@Override
	public Image getOpenedIcon(int type) {
		return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
				"tree." + getClass().getSimpleName()); //$NON-NLS-1$
	}

	@Override
	public SystemAction getDefaultAction() {
		return SystemAction.get(EditAction.class);
	}

	/**
	 * @return
	 */
	public LDAPConnectionDescriptor getConnectionDescriptor() {
		return (LDAPConnectionDescriptor) getLookup().lookup(
				LDAPConnectionDescriptor.class);
	}
}
