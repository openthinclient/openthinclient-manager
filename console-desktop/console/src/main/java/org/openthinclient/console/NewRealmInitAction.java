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

import java.awt.event.ActionEvent;
import java.io.Serializable;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openide.util.HelpCtx;

import com.levigo.util.swing.IconManager;

/**
 * @author bohnerne
 */
public class NewRealmInitAction extends AbstractAction implements Serializable {

	private static final long serialVersionUID = 1L;
	private final NewRealmInitCommand delegate;

	public NewRealmInitAction() {
		super(Messages.getString("action."
				+ NewRealmInitAction.class.getSimpleName()), new ImageIcon(
				NewRealmInitAction.class.getResource("otc-new.png")));
		delegate = new NewRealmInitCommand();
	}

	public void actionPerformed(ActionEvent ae) {
		delegate.execute(null);
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	// @Override
	public String getName() {
		return Messages.getString("action." //$NON-NLS-1$
				+ NewRealmInitAction.class.getSimpleName());
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getHelpCtx()
	 */
	// @Override
	public HelpCtx getHelpCtx() {
		return null;
	}

	// @Override
	public Icon getOpenedIcon() {
		return new ImageIcon(IconManager.getInstance(DetailViewProvider.class,
				"icons").getImage( //$NON-NLS-1$
				"tree." + getClass().getSimpleName()));
	}
}