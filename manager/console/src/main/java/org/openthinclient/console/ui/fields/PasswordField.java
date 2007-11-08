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
package org.openthinclient.console.ui.fields;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.schema.PasswordNode;


/**
 * @author Joerg Henne
 */
public class PasswordField extends ConfigField {
  private JTextField textField;

  public PasswordField(Profile profile, PasswordNode n) {
    super(profile, n);

    textField = new JPasswordField();

    updateRepresentation();

    textField.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
    	  if(textField.getText().equals("")) {
    		  resetValueToDefault();
    	  }else {
    		  setValue(textField.getText());
    	  }
      }
    });
  }

  /**
   * @param treeNode
   */
  protected void updateRepresentation() {
    final String value = profile.getValue(node.getKey());
    textField.setText(value != null ? value : "");

    super.updateRepresentation();
  }

  /*
   * @see org.openthinclient.ui.ConfigField#getMainComponent()
   */
  protected JComponent getMainComponent() {
    return textField;
  }
}
