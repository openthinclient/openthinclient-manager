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
package org.openthinclient.common.model.schema;

import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "choice-node")
@XmlAccessorType(XmlAccessType.NONE)
public class ChoiceNode extends EntryNode {
  private static final long serialVersionUID = 983752831232339241L;
  @XmlElement(name = "option")
  private final List<Option> options = new ArrayList<Option>();
  private Style style;

  public List<Option> getOptions() {
    return options;
  }

  public void addOption(Option option) {
    options.add(option);
  }

  /**
   * Return the currently selected option, i.e. the option with a value equal to
   * the current node value.
   */
  public Option getSelectedOption() {
    final String value = getValue();

    for (final Option o : options)
      if (o.getValue().equals(value))
        return o;

    return null;
  }

  /**
   * Return the localized label for a given value
   */
  public String getLabelForValue(String value) {
    if (null != value)
      for (final Option option : getOptions())
        if (value.equals(option.getValue()))
          return option.getLabel();

    return "";
  }

  /*
   * @see org.openthinclient.Node#toStringExtended(int, java.lang.StringBuffer)
   */
  protected void toStringExtended(int indent, StringBuffer sb) {
    for (final Option option : options) {
      for (int j = 0; j < indent; j++)
        sb.append("  ");
      sb.append("[Option: ").append(option.getName()).append(" -> ").append(
              option.getValue()).append("]\n");
    }
  }

  public Style getStyle() {
    return style;
  }

  public void setStyle(String style) {
    this.style = Style.valueOf(style);
  }

  public void setStyle(Style style) {
    this.style = style;
  }

  public enum Style {
    COMBOBOX, LIST, LIST_MULTI, CHECKBOXES, RADIOBUTTONS
  }

  @XmlType(name = "choice-node-option")
  @XmlAccessorType(XmlAccessType.NONE)
  public static class Option implements Serializable {
    private static final long serialVersionUID = 83274659873458345L;
    @XmlElement(name = "label")
    private final List<Label> labels = new ArrayList<>();
    @XmlAttribute(name = "name")
    private String name;
    @XmlAttribute(name = "value")
    private String value;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return getLabel();
    }

    public List<Label> getLabels() {
      return labels;
    }

    public String getLabel() {
      for (final Label label : labels)
        if (label.getLang().equals(Locale.getDefault().getLanguage())) {
          final String labelText = label.getLabel();
          LoggerFactory.getLogger(this.getClass()).debug(labelText);
          if (labelText != null)
            return labelText;
        }
      return getName();
    }
  }
}
