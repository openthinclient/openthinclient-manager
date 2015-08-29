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
package org.openthinclient.service.nfs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;

/**
 * The sole purpose of this property editor is to enable the JBoss XMBean
 * persistence to persist the exports in a readable way.
 * 
 * @author levigo
 */
public class ExportsEditor implements PropertyEditor {
	private static final Logger LOG = LoggerFactory.getLogger(ExportsEditor.class);

	private Exports value;

	public void setValue(Object value) {
		this.value = (Exports) value;
	}

	public Object getValue() {
		return value;
	}

	public boolean isPaintable() {
		return false;
	}

	public void paintValue(Graphics gfx, Rectangle box) {
	}

	public String getJavaInitializationString() {
		return null;
	}

	public String getAsText() {
		final StringBuffer sb = new StringBuffer();
		for (final NFSExport export : value)
			sb.append(export).append(";");
		return sb.toString();
	}

	public void setAsText(String text) throws IllegalArgumentException {

		final ExportsParser parser = new ExportsParser();

		value = new Exports();
		final String specs[] = text.split("\\s*;\\s*");
		for (final String spec : specs)
			try {
				value.add(parser.parse(spec));
			} catch (final Exception e) {
				LOG.warn("Ignoring export spec because of", e);
			}
	}

	public String[] getTags() {
		return null;
	}

	public Component getCustomEditor() {
		return null;
	}

	public boolean supportsCustomEditor() {
		return false;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
	}
}
