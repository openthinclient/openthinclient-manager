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

import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.console.Messages;
import org.openthinclient.console.util.DetailViewFormBuilder;

import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Natalie Bohnert
 */
public class UnrecognizedClientView extends AbstractViewPanel {
  /*
   * @see org.openthinclient.console.ObjectEditorPart#getMainComponent()
   */
  public UnrecognizedClientView(final UnrecognizedClient client) {
    DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
        "r:p, 3dlu, f:p:g"), Messages.getBundle(), this); //$NON-NLS-1$
    dfb.getPanel().setName(Messages.getString("Settings_title")); //$NON-NLS-1$
    
    appendRow(dfb, "Client.ipHostNumber", client.getIpHostNumber()); //$NON-NLS-1$
    appendRow(dfb, "Client.macAddress", client.getMacAddress()); //$NON-NLS-1$
  }
}
