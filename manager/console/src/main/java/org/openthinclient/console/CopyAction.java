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
package org.openthinclient.console;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.DirectoryFacade;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

/**
 * @author levigo
 */

public class CopyAction extends NodeAction {

	public CopyAction() {
		super();
	}

	@Override
	protected void performAction(Node[] arg0) {
		for (final Node node : arg0)
			try {
				final DirectoryObject dirObject = (DirectoryObject) node.getLookup()
						.lookup(DirectoryObject.class);

				final Realm realm = (Realm) node.getLookup().lookup(Realm.class);

				// FIXME: make this work for object in the secondary directory as well.
				final LDAPConnectionDescriptor lcd = realm.getConnectionDescriptor();
				final DirectoryFacade df = lcd.createDirectoryFacade();
				final LdapContext ctx = df.createDirContext();
				try {
					final DirectoryObject copy = dirObject.getClass().newInstance();

					// save new Object (Namen anders finden)
					copy.setName("copy"); // !!!!!

					realm.getDirectory().save(copy);

					// find and save Attribute
					final Name name = df.makeRelativeName(dirObject.getDn());

					final String dnOU = copy.getDn().replace("," + lcd.getBaseDN(), "");

					// String dnOU = name.getPrefix(name.size() - 1).toString() ;
					// String dn = "";
					// if(null != dnOU && !dnOU.equals("")){
					// dn = "cn=" + copy.getName() + ","+ dnOU;
					// }
					final Name nameNew = df.makeRelativeName(dnOU);

					final Attributes dirObjAttrs = ctx.getAttributes(name);

					final NamingEnumeration<? extends Attribute> enm = dirObjAttrs
							.getAll();

					final List<ModificationItem> mods = new LinkedList<ModificationItem>();

					while (enm.hasMore()) {
						final Attribute a = enm.next();

						if (a.getID().equals("cn"))
							continue;

						mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, a));
					}

					if (mods.size() > 0) {
						final ModificationItem mi[] = new ModificationItem[mods.size()];
						mods.toArray(mi);
						ctx.modifyAttributes(nameNew, mi);
					}

					// member - groups
					final Class[] classes = new Class[]{Realm.class, UserGroup.class,
							ApplicationGroup.class, Application.class, Printer.class,
							Device.class, Location.class};

					final Set<DirectoryObject> set = new HashSet<DirectoryObject>();

					for (final Class cl : classes) {
						final Set<DirectoryObject> list = realm.getDirectory().getMapping()
								.list(cl);
						set.addAll(list);
					}

					for (final DirectoryObject obj : set) {
						final Name targetName = df.makeRelativeName(obj.getDn());

						final Attributes attrs = ctx.getAttributes(targetName);

						final Attribute a = attrs.get("uniquemember");

						if (a != null)
							for (int i = 0; a.size() > i; i++)
								if (a.get(i).equals(LDAPDirectory.idToUpperCase(dirObject.getDn()))) {

									if (a.size() == 0) {
										// FIXME: don't deal with the dummy member here
										// final String dummy = OneToManyMapping.getDUMMY_MEMBER();
										// a.add(dummy);
									}

									final Attribute newMember = (Attribute) a.clone();

									newMember.add(LDAPDirectory.idToUpperCase(copy.getDn()));

									final ModificationItem[] mod = new ModificationItem[1];
									mod[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
											newMember);

									ctx.modifyAttributes(targetName, mod);
								}
					}
				} finally {
					ctx.close();
				}

				final Node parentNode = node.getParentNode();
				if (null != parentNode && parentNode instanceof Refreshable)
					((Refreshable) parentNode).refresh();

				// if (node instanceof Refreshable)
				// ((Refreshable) node).refresh();
			} catch (final DirectoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (final NamingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (final InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (final IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	@Override
	protected boolean enable(Node[] activatedNodes) {
		for (final Node node : activatedNodes) {
			final Class currentClass = (Class) node.getLookup().lookup(Class.class);
			if (!LDAPDirectory.isMutable(currentClass))
				return false;
		}
		return true;
	}

	@Override
	public String getName() {
		return Messages.getString("Copy");
	}

	@Override
	public HelpCtx getHelpCtx() {
		return null;
	}
}
