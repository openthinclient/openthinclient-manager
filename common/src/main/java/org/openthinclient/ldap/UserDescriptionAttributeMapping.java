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

import java.lang.reflect.Method;

/** Special mapping for dynamically configurable username attribute.
 *
 * This belongs only to User objects.
 */
public class UserDescriptionAttributeMapping extends AttributeMapping {

  public UserDescriptionAttributeMapping(String fieldName)
      throws ClassNotFoundException {
    super(fieldName, "java.lang.String");
  }

  @Override
  protected Method getGetter() throws NoSuchMethodException {
    return type.getMappedType().getMethod("getUserDescription");
  }

  @Override
  protected Method getSetter() throws NoSuchMethodException {
    return type.getMappedType().getMethod("setUserDescription", String.class);
  }
}
