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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openthinclient.common.model.schema.Schema;

/**
 * @author levigo
 */
public class HTTPSchemaProvider extends AbstractSchemaProvider {
	private static final Logger logger = Logger.getLogger(HTTPSchemaProvider.class);

	private final URL baseURL;

	/**
	 * @throws MalformedURLException
	 * 
	 */
	public HTTPSchemaProvider(String hostname) throws MalformedURLException {
		// TODO: JN Schema über HTTP? 
		baseURL = new URL("http", hostname, 8080, "/openthinclient/files/" + SCHEMA_PATH + "/");
		if (logger.isDebugEnabled())
			logger.debug("Using schema base url: " + baseURL);
	}

	public boolean checkAccess() {
		try {
			URLConnection openConnection = baseURL.openConnection();
			String contentType = openConnection.getContentType();
			return contentType.startsWith("text/plain;");
		} catch (Exception e) {
			return false;
		}
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
			loadFromURL(schemas, new URL(baseURL, profileTypeName + ".xml"));
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
	private void loadFromURL(List<Schema> schemas, URL url) throws IOException,
			SchemaLoadingException {
		if (logger.isDebugEnabled())
			logger.debug("Trying to load schema from " + url);

		URLConnection con = url.openConnection();
		if (con.getContentType().startsWith("application/octet-stream"))
			schemas.add(loadSchema(con.getInputStream()));
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
			URL dirURL = new URL(baseURL, profileTypeName + "/");
			if (logger.isDebugEnabled())
				logger.debug("Trying to load all schemas for " + profileTypeName
						+ " from " + dirURL);
			URLConnection con = dirURL.openConnection();
			InputStream inputStream = con.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream,
					"ISO-8859-1"));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("F")) {
					String filename = line.substring(2);

					if (filename.endsWith(".xml"))
						loadFromURL(schemas, new URL(dirURL, filename));
				}
			}
			br.close();
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

	@Override
	public void reload() {
		super.reload();
		try {
			loadAllSchemas("application");
		} catch (SchemaLoadingException e) {
			e.printStackTrace();
		}
	}
}
