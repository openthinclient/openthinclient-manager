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
package org.openthinclient.common.model.schema.provider;

import org.openthinclient.common.model.schema.Schema;

/**
 * @author Natalie Bohnert Interface for classes that provide schema handling
 *         for different profiles.
 */
public interface SchemaProvider {

  /**
   * Returns all schema types for the given profile type.
   * 
   * @param profileType The concrete class of the Profile e.g. Application,
   *          Device ...
   * @return All schema types for this profile type
   */
  public String[] getSchemaNames(Class profileType) throws SchemaLoadingException;

  /**
   * Returns a new instance of Schema for the given profile type and schema type
   * 
   * @param profileType The concrete class of the Profile e.g. Application,
   *          Device ...
   * @param schemaName The name of schema type e.g. AcrobatReader ...
   * @return New instance of Schema
   */
  public Schema getSchema(Class profileType, String schemaName) throws SchemaLoadingException;
  
}
