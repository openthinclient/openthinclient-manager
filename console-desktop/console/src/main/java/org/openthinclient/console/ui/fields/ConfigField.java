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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComponent;

import org.openide.awt.StatusDisplayer;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.schema.Node;


/**
 * @author levigo
 */
public abstract class ConfigField {
  // private FormLayout layout = new FormLayout("pref:grow, 3dlu, pref",
  // "pref");

  private static class MyFocusListener implements FocusListener {
    private String didSetThisText = null;

    /*
     * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
     */
    public void focusGained(FocusEvent e) {
      String ttt = ((JComponent) e.getSource()).getToolTipText();
      StatusDisplayer.getDefault().setStatusText(null != ttt ? ttt : ""); //$NON-NLS-1$
      didSetThisText = ttt;
    }

    /*
     * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
     */
    public void focusLost(FocusEvent e) {
      if (null != didSetThisText
          && didSetThisText
              .equals(StatusDisplayer.getDefault().getStatusText()))
        StatusDisplayer.getDefault().setStatusText(""); //$NON-NLS-1$
      didSetThisText = null;
    }
  }

  private static MyFocusListener myFocusListener = new MyFocusListener();

  protected Node node;
  protected final Profile profile;

  // private JButton resetButton = new JButton();
  // protected Context context;

  protected ConfigField(Profile profile, Node node) {
    this.profile = profile;
    this.node = node;
  }

  /**
   * 
   */
  protected abstract JComponent getMainComponent();

  public JComponent getEditor() {
    JComponent mainComponent = getMainComponent();

    mainComponent.setToolTipText(node.getTip());
    mainComponent.addFocusListener(myFocusListener);

    // context = new Context(this, Context.NO_CHILDREN);
    // context.add(this);

    return mainComponent;
  }

  /**
   * @return
   */
  public boolean isValueOverridden() {
    return profile.containsValue(node.getKey());
  }

  public void resetValueToDefault() {
    profile.removeValue(node.getKey());
    updateRepresentation();
  }

  protected void updateRepresentation() {
    // String toolTip;
    // if (isValueOverridden())
    // toolTip = "Wert \""
    // + valueToDisplayedValue(profile.getOverriddenValue(node.getKey()))
    // + "\" aus \"" + profile.getDefiningProfile(node.getKey())
    // + "\" ist �berschrieben";
    // else
    // toolTip = "Voreinstellung aus \""
    // + profile.getDefiningProfile(node.getKey()) + "\" wird �bernommen";
    //
    // resetButton.setToolTipText(toolTip);
  }

  /**
   * @param overriddenValue
   * @return
   */
  protected String valueToDisplayedValue(String value) {
    return value;
  }

  /**
   * 
   */
  protected void setValue(String value) {
    if (null == profile.getValue(node.getKey())
        || !profile.getValue(node.getKey()).equals(value))
      profile.setValue(node.getKey(), value);
    updateRepresentation();
  }
}
