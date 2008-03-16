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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

/**
 * @author levigo
 */
public class ChoiceNode extends EntryNode {
	private static final long serialVersionUID = 983752831232339241L;

	public static class Option implements Serializable {
		private static final long serialVersionUID = 83274659873458345L;

		private String name;
		private String value;

		private ArrayList<Label> labels;

		public Option(String value) {
			this.value = value;
		}

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

		public ArrayList<Label> getLabels() {
			return labels;
		}

		public void setLabels(ArrayList<Label> labels) {
			this.labels = labels;
		}

		public String getLabel() {
			if (labels != null)
				for (final Label label : labels)
					if (label.getLang().equals(Locale.getDefault().getLanguage())) {
						final String labelText = label.getLabel();
						Logger.getLogger(this.getClass()).debug(labelText);
						if (labelText != null)
							return labelText;
					}
			return getName();
		}
	}

	private final List<Option> options = new ArrayList<Option>();

	public enum Style {
		COMBOBOX, LIST, LIST_MULTI, CHECKBOXES, RADIOBUTTONS
	}

	private Style style;

	/**
	 * @param name
	 * @param iniName
	 * @param value
	 */
	public ChoiceNode(String name, String value) {
		super(name, value);
	}

	public List<Option> getOptions() {
		return options;
	}

	public void addOption(Option option) {
		options.add(option);
	}

	/**
	 * Return the currently selected option, i.e. the option with a value equal to
	 * the current node value.
	 * 
	 * @return
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
	 * 
	 * @param value
	 * @return
	 */
	public String getLabelForValue(String value) {
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

	public void setStyle(Style style) {
		this.style = style;
	}

	public void setStyle(String style) {
		this.style = Style.valueOf(style);
	}

	/*
	 * @see org.openthinclient.common.model.schema.EntryNode#getUID()
	 */
	@Override
	public long getUID() {
		long uid = 0;
		for (final Iterator i = options.iterator(); i.hasNext();) {
			final Option opt = (Option) i.next();
			uid ^= opt.getValue().hashCode();
		}

		return super.getUID() ^ uid;
	}
}
