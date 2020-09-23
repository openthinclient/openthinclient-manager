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

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Profile;

import java.text.Collator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The root node of a configuration template.
 */
@XmlType(name = "schema")
@XmlRootElement(name = "schema")
@XmlAccessorType(XmlAccessType.NONE)
public class Schema extends Node implements Comparable<Schema> {
  private static final long serialVersionUID = 109860938274823423L;
  private final String subtype;
  private Class type;

  /**
   * DEPRECATED: single-instance-only support has never been added to the schema system.
   */
  @SuppressWarnings("unused")
  @XmlAttribute(name = "single-instance-only")
  @Deprecated
  private Boolean deprecatedSingleInstanceOnly;

  /**
   * DEPRECATED: previous versions of the schema allowed to specify the type of schema used. This is
   * only here for backwards compatibility and will not be used anymore.
   */
  @SuppressWarnings("unused")
  @XmlAttribute(name = "type")
  @Deprecated
  private String deprecatedType;

  /**
   * DEPRECATED: previous versions of the schema allowed to specify the version of schema used. The
   * current implementation will compute a dynamic version number. This is only here for backwards
   * compatibility and will not be used anymore.
   */
  @SuppressWarnings("unused")
  @XmlAttribute(name = "version")
  @Deprecated
  private String deprecatedVersion;

  public Schema() {
    this.type = Application.class;
    this.subtype = null;
  }

  public Schema getSchema() {
    return this;
  }

  /*
   * @see org.openthinclient.common.model.Profile#contains(java.lang.String)
   */
  public boolean contains(String key) {
    return null != getChild(key);
  }

  /*
   * @see org.openthinclient.common.model.Profile#getKeys()
   */
  public Set<String> getKeys() {
    return null;
  }

  /*
   * @see
   * org.openthinclient.common.model.Profile#getOverriddenValue(java.lang.String
   * )
   */
  public String getOverriddenValue(String key) {
    return null;
  }

  /*
   * @see org.openthinclient.common.model.Profile#getValue(java.lang.String,
   * boolean)
   */
  public String getValue(String key) {
    final Node n = getChild(key);
    return null != n && n instanceof EntryNode
            ? ((EntryNode) n).getValue()
            : null;
  }

  /*
   * @see org.openthinclient.common.model.Profile#getValues()
   */
  public Map<String, String> getValues() {
    return null;
  }

  /*
   * @see org.openthinclient.common.model.Profile#setValues(java.util.SortedMap)
   */
  public void setValues(SortedMap<String, String> values) {
    throw new IllegalArgumentException("Can't set the value here.");
  }

  /*
   * @see org.openthinclient.common.model.Profile#setValue(java.lang.String,
   * java.lang.String)
   */
  public void setValue(String path, String value) {
    throw new IllegalArgumentException("Can't set the value here.");
  }

  public Class getType() {
    return type;
  }

  public void setType(Class type) {
    this.type = type;
  }

  public String getSubtype() {
    return subtype;
  }

  public long getVersion() {
    return getUID();
  }

  public int compareTo(Schema compareSchema) {
    final Collator collator = Collator.getInstance();
    return collator.compare(this.getLabel(), compareSchema.getLabel());
  }

}
