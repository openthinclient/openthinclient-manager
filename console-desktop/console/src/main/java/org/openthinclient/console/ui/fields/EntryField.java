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
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.schema.EntryNode;
import org.openthinclient.console.Messages;


/**
 * @author levigo
 */
public class EntryField extends ConfigField {
  private JTextField textField = new JTextField();

  public EntryField(Profile profile, EntryNode n) {
    super(profile, n);
    
    if(n.getKey().equals("Serversettings.Hostname") || n.getKey().equals("Serversettings.Portnumber")) {
    	textField.setEditable(false);
    }
    
    updateRepresentation();

    textField.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        String v = textField.getText();
        if (v.length() > 0)
          setValue(v);
        else
          resetValueToDefault();
      }

      /*
       * @see java.awt.event.FocusAdapter#focusGained(java.awt.event.FocusEvent)
       */
      @Override
      public void focusGained(FocusEvent e) {
        updateRepresentation();
      }
    });
  }

  /**
   * @param treeNode
   */
  protected void updateRepresentation() {
    final String k = node.getKey();
    final String value = profile.getValue(k);
    final boolean containsValue = profile.containsValue(k);
    final String definingProfile = profile.getDefiningProfile(k, true);

    String text;
    if (containsValue || textField.hasFocus()) {
      textField.setForeground(UIManager.getColor("TextField.foreground")); //$NON-NLS-1$
      if (textField.hasFocus())
        text = containsValue && null != value ? value : ""; //$NON-NLS-1$
      else {
        text = value;

        final String overriddenValue = profile.getOverriddenValue(k);
        if (null != overriddenValue)
          text += " " + Messages.getString("EntryField.overrides", overriddenValue, //$NON-NLS-1$ //$NON-NLS-2$
              definingProfile);
      }
    } else {
      textField.setForeground(UIManager
          .getColor("TextField.inactiveForeground")); //$NON-NLS-1$
      if (null != value)
        text = value
            + " " +Messages.getString("EntryField.defaultFrom", definingProfile); //$NON-NLS-1$ //$NON-NLS-2$
      else
        text = Messages.getString("EntryField.noDefault"); //$NON-NLS-1$
    }

    textField.setText(text);

    super.updateRepresentation();
  }

  /*
   * @see org.openthinclient.ui.ConfigField#getMainComponent()
   */
  protected JComponent getMainComponent() {
    return textField;
  }
}
