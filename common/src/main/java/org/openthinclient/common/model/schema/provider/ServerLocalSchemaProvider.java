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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openthinclient.common.model.schema.Schema;

/**
 * @author levigo
 */
public class ServerLocalSchemaProvider extends AbstractSchemaProvider {
	private static final Logger logger = Logger.getLogger(ServerLocalSchemaProvider.class);
	private final File basedir;

	/**
	 * @throws MalformedURLException
	 * 
	 */
	public ServerLocalSchemaProvider() {
		// TODO: JN: System.getProperty()/System.getenv() sollte über Config gesetzt werden
		basedir = new File(System.getenv("manager.home"), "nfs" + File.separator + "root" + File.separator + SCHEMA_PATH);
		if (logger.isDebugEnabled())
			logger.debug("Using schema base dir: " + basedir);
	}

	/**
	 * @param profileTypeName
	 * @return
	 * @throws SchemaLoadingException
	 */
	@Override
	protected List<Schema> loadDefaultSchema(String profileTypeName)
			throws SchemaLoadingException {
		List<Schema> schemas = new ArrayList<Schema>();

		try {
			loadFromFile(schemas, new File(basedir, profileTypeName + ".xml"));
		} catch (Throwable e) {
			throw new SchemaLoadingException(
					"Could not fetch schema from file service", e);
		}
		return schemas;
	}

	/**
	 * @param schemas
	 * @param url
	 * @throws IOException
	 * @throws SchemaLoadingException
	 */
	private void loadFromFile(List<Schema> schemas, File f) throws IOException,
			SchemaLoadingException {
		if (logger.isDebugEnabled())
			logger.debug("Trying to load schema from " + f);

		if (f.exists() && f.canRead())
			schemas.add(loadSchema(new FileInputStream(f)));
	}

	/**
	 * @param profileTypeName
	 * @return
	 * @throws SchemaLoadingException
	 */
	@Override
	protected List<Schema> loadAllSchemas(String profileTypeName)
			throws SchemaLoadingException {
		List<Schema> schemas = new ArrayList<Schema>();

		try {
			File dir = new File(basedir, profileTypeName);
			if (logger.isDebugEnabled())
				logger.debug("Trying to load all schemas for " + profileTypeName
						+ " from " + dir);

			File[] files = dir.listFiles(new FileFilter() {
				public boolean accept(File f) {
					return f.isFile() && f.canRead() && f.getName().endsWith(".xml");
				}
			});

			if (null != files)
				for (int i = 0; i < files.length; i++) {
					File f = files[i];
					loadFromFile(schemas, f);
				}
		} catch (FileNotFoundException e) {
			// just ignore it.
			if (logger.isDebugEnabled())
				logger.debug("No schemas found for " + profileTypeName);
		} catch (Throwable e) {
			logger.error("Could not fetch schema from file service", e);
			throw new SchemaLoadingException(
					"Could not fetch schema from file service", e);
		}

		return schemas;
	}
}
