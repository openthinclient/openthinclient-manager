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
package org.openthinclient.ldap;

import java.util.regex.Pattern;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

/**
 * A special mapping for the RDN attribute which quotes/unquotes the name in
 * order to satisfy LDAP name escaping rules.
 * 
 * @author levigo
 */
public class RDNAttributeMapping extends AttributeMapping {

  /**
   * @param fieldName
   * @param fieldType
   * @throws ClassNotFoundException
   */
  public RDNAttributeMapping(String fieldName)
      throws ClassNotFoundException {
    super(fieldName, "java.lang.String");
  }

  private static final Pattern QUOTE_TO_LDAP = Pattern.compile("[\\\\,=]");
  private static final String QUOTE_REPLACEMENT = "\\\\$0";

  /*
   * @see org.openthinclient.ldap.AttributeMapping#valueToAttributes(javax.naming.directory.BasicAttributes,
   *      java.lang.Object)
   */
  @Override
  protected Object valueToAttributes(BasicAttributes a, Object v) {
    assert v instanceof String;
    return super.valueToAttributes(a, QUOTE_TO_LDAP.matcher((String) v)
        .replaceAll(QUOTE_REPLACEMENT));
  }

  /** The Pattern used to un-quote a value from ldap escaping */
  private static final Pattern UNQUOTE_FROM_LDAP = Pattern.compile("\\",
      Pattern.LITERAL);

  /*
   * @see org.openthinclient.ldap.AttributeMapping#valueFromAttributes(javax.naming.directory.Attributes,
   *      java.lang.Object)
   */
  @Override
  protected Object valueFromAttributes(Attributes a, Object o, Transaction tx)
      throws NamingException, DirectoryException {
    Object value = super.valueFromAttributes(a, o, tx);
    if (value instanceof String)
      return UNQUOTE_FROM_LDAP.matcher((String) value).replaceAll("");
    else
      return value;
  }
}
