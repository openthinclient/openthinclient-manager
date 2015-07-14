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
package org.openthinclient.common.model.schema.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.Unmarshaller;
import org.openthinclient.common.model.schema.Node;
import org.openthinclient.common.model.schema.Schema;
import org.xml.sax.InputSource;

/**
 * @author levigo
 */
public abstract class AbstractSchemaProvider implements SchemaProvider {
	private static final Logger logger = Logger.getLogger(AbstractSchemaProvider.class);

	protected final String SCHEMA_PATH = "schema";
	protected Map<String, Map<String, Schema>> typeCache = new HashMap<String, Map<String, Schema>>();
	private Mapping mapping;

	/**
	 * @throws SchemaLoadingException
	 * @see SchemaProvider
	 */
	public Schema getSchema(Class profileType, String schemaName)
			throws SchemaLoadingException {
		try {
			String profileTypeName = profileType.getName().toLowerCase();
			profileTypeName = profileTypeName.substring(profileTypeName
					.lastIndexOf('.') + 1);

			if (!typeCache.containsKey(profileTypeName))
				getSchemaNames(profileType);

			final Map<String, Schema> schemas = typeCache.get(profileTypeName);

			if (null != schemaName && schemas.containsKey(schemaName))
				return schemas.get(schemaName);

			return schemas.get(profileTypeName);
		} catch (final Exception e) {
			logger.error("Schema couldn't be loaded! " + e);
		}
		return null;
	}

	/**
	 * Lazily load the mapping. Create it, if necessary.
	 * 
	 * @return
	 * @throws IOException
	 * @throws MappingException
	 */
	private Mapping getMapping() throws IOException, MappingException {
		if (null == mapping) {
			mapping = new Mapping();
			mapping.loadMapping(new InputSource(Node.class
					.getResourceAsStream("schema-mapping.xml")));
		}
		return mapping;
	}

	/**
	 * @see SchemaProvider
	 * @return All types of profile from xml files stored in folder named like
	 *         class name of profile type.
	 */
	@SuppressWarnings("unchecked")
	public String[] getSchemaNames(Class profileType)
			throws SchemaLoadingException {
		String profileTypeName = profileType.getName().toLowerCase();
		profileTypeName = profileTypeName.substring(profileTypeName
				.lastIndexOf('.') + 1);
		if (!typeCache.containsKey(profileTypeName)) {
			final List<Schema> schemas = new ArrayList<Schema>();

			// get contents of directory named after the profile type name
			schemas.addAll(loadAllSchemas(profileTypeName));

			// get the default schema for the profile type
			schemas.addAll(loadDefaultSchema(profileTypeName));

			// cache 'em all
			Map<String, Schema> schemasByName = typeCache.get(profileTypeName);
			if (null == schemasByName) {
				schemasByName = new HashMap<String, Schema>();
				typeCache.put(profileTypeName, schemasByName);
			}

			for (final Schema schema : schemas)
				schemasByName.put(schema.getName(), schema);
		}

		final Map schemas = typeCache.get(profileTypeName);
		if (null == schemas) {
			logger.error("No schemas found for " + profileType);
			return new String[0];
		}
		final String[] keys = new String[schemas.keySet().size()];
		return (String[]) schemas.keySet().toArray(keys);
	}

	protected abstract List<Schema> loadDefaultSchema(String profileTypeName)
			throws SchemaLoadingException;

	protected abstract List<Schema> loadAllSchemas(String profileTypeName)
			throws SchemaLoadingException;

	protected Schema loadSchema(InputStream is) throws SchemaLoadingException {
		try {
			final Mapping mapping = getMapping();

			// read using an input source, so that the xml parser wil.
			// use the encoding specification from the file.
			final InputSource source = new InputSource(is);

			// Create a new Unmarshaller
			final Unmarshaller unmarshaller = new Unmarshaller();
			unmarshaller.setMapping(mapping);

			// Unmarshal the schema
			return (Schema) unmarshaller.unmarshal(source);
		} catch (final Exception e) {
			throw new SchemaLoadingException("Schema couldn't be loaded!", e);
		}
	}

	public void reload() {
		typeCache = new HashMap<String, Map<String, Schema>>();
	}
}
