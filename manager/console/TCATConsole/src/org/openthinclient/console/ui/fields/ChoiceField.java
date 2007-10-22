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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.schema.ChoiceNode;
import org.openthinclient.common.model.schema.ChoiceNode.Option;
import org.openthinclient.console.Messages;


/**
 * @author levigo
 */
public class ChoiceField extends ConfigField {
  /**
   * A list cell renderer which renders the first entry as "disabled"
   */
  public class MyListCellRenderer extends BasicComboBoxRenderer {
    /*
     * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList,
     *      java.lang.Object, int, boolean, boolean)
     */
    public Component getListCellRendererComponent(JList list, Object value,
        int index, boolean isSelected, boolean cellHasFocus) {
      setEnabled(index != 0);
      return super.getListCellRendererComponent(list, value, index, isSelected,
          cellHasFocus);
    }
  }

  private JComboBox comboBox;

  private static class ChoiceNodeModel extends DefaultComboBoxModel {
    private ChoiceNode node;

    /**
     * @param n
     */
    public ChoiceNodeModel(ChoiceNode n) {
      this.node = n;
    }

    /*
     * @see javax.swing.DefaultComboBoxModel#getElementAt(int)
     */
    public Object getElementAt(int index) {
      if (index == 0) {
        Option selectedOption = node.getSelectedOption();
        if (null != selectedOption)
          return selectedOption.getLabel() + Messages.getString("ChoiceField.default"); //$NON-NLS-1$
        else
          return "<?>"; //$NON-NLS-1$
      } else
        return node.getOptions().get(index - 1);
    }

    /*
     * @see javax.swing.DefaultComboBoxModel#getIndexOf(java.lang.Object)
     */
    public int getIndexOf(Object anObject) {
      return node.getOptions().indexOf(anObject) + 1;
    }

    /*
     * @see javax.swing.DefaultComboBoxModel#getSize()
     */
    public int getSize() {
      return node.getOptions().size() + 1;
    }
  }

  public ChoiceField(Profile profile, ChoiceNode n) {
    super(profile, n);

    comboBox = new JComboBox();
    comboBox.setModel(new ChoiceNodeModel((ChoiceNode) node));

    updateRepresentation();

    comboBox.setRenderer(new MyListCellRenderer());

    comboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (comboBox.getSelectedIndex() == 0) {
          resetValueToDefault();
          updateRepresentation();
        } else
          setValue(((ChoiceNode.Option) comboBox.getModel().getSelectedItem())
              .getValue());
      }
    });
  }

  /**
   * @param treeNode
   */
  protected void updateRepresentation() {
    getMainComponent();

    if (isValueOverridden()) {
      String currentValue = profile.getValue(node.getKey());
      // loop starts at element 1, since 0 is the default
      for (int i = 1; i < comboBox.getModel().getSize(); i++) {
        ChoiceNode.Option option = (ChoiceNode.Option) comboBox.getModel()
            .getElementAt(i);
        if (option.getValue().equals(currentValue))
          comboBox.setSelectedIndex(i);
      }
      comboBox.setForeground(UIManager.getColor("ComboBox.foreground")); //$NON-NLS-1$
    } else {
      comboBox.setSelectedIndex(0);
      comboBox.setForeground(UIManager.getColor("ComboBox.disabledForeground")); //$NON-NLS-1$
    }

    super.updateRepresentation();
  }

  /*
   * @see org.openthinclient.ui.ConfigField#setValue(java.lang.String)
   */
  protected void setValue(String value) {
    profile.setValue(node.getKey(), value);
    updateRepresentation();
  }

  /*
   * @see org.openthinclient.ui.ConfigField#getMainComponent()
   */
  protected JComponent getMainComponent() {
    return comboBox;
  }

  /*
   * @see org.openthinclient.ui.ConfigField#valueToDisplayedValue(java.lang.String)
   */
  protected String valueToDisplayedValue(String value) {
    for (int i = 0; i < comboBox.getModel().getSize(); i++) {
      Object listElement = comboBox.getModel().getElementAt(i);
      if (!(listElement instanceof ChoiceNode.Option))
        return listElement.toString();

      ChoiceNode.Option option = (ChoiceNode.Option) listElement;
      if (option.getValue().equals(value))
        return option.getLabel();
    }

    return value + "(?)"; //$NON-NLS-1$
  }
}
