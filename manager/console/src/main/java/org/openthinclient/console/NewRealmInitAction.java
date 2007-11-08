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

import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;

/**
 * @author bohnerne
 */
@SuppressWarnings("serial")
public class NewRealmInitAction extends CallableSystemAction {

	private final NewRealmInitCommand delegate;

	public NewRealmInitAction() {
		delegate = new NewRealmInitCommand();
	}

	/*
	 * @see org.openide.util.actions.CallableSystemAction#asynchronous()
	 */
	@Override
	protected boolean asynchronous() {
		return true;
	}

	/*
	 * @see org.openide.util.actions.CallableSystemAction#performAction()
	 */
	@Override
	public void performAction() {
		delegate.execute(null);
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		// System.out.println(Messages.getString("action." //$NON-NLS-1$
		// + NewRealmInitAction.class.getSimpleName()));
		return Messages.getString("action." //$NON-NLS-1$
				+ NewRealmInitAction.class.getSimpleName());
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getHelpCtx()
	 */
	@Override
	public HelpCtx getHelpCtx() {
		return null;
	}

	@Override
	protected String iconResource() {
		return "org/openthinclient/console/otc-new.png";
	}

}