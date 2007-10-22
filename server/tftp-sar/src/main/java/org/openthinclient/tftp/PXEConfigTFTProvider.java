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

package org.openthinclient.tftp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.directory.server.core.configuration.Configuration;
import org.apache.directory.server.core.configuration.SyncConfiguration;
import org.apache.directory.shared.ldap.util.Base64;
import org.apache.log4j.Logger;
import org.openthinclient.common.directory.LDAPConnectionDescriptor;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.common.util.UsernamePasswordHandler;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.tftp.tftpd.TFTPProvider;


/**
 * 
 * @author grafvr
 * 
 */
public class PXEConfigTFTProvider implements TFTPProvider {

	private static final Logger logger = Logger
			.getLogger(PXEConfigTFTProvider.class);

	private Set<Realm> realms;

	private URL templateURL;

	public PXEConfigTFTProvider() throws DirectoryException {
		init();
	}

  /**
   * @throws DirectoryException
   */
  private void init() throws DirectoryException {
    LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
    lcd
        .setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
    lcd
        .setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
    lcd.setCallbackHandler(new UsernamePasswordHandler("uid=admin,ou=system",
        "secret".toCharArray()));

		try {
			realms = LDAPDirectory.findAllRealms(lcd);
			logger.info("----------------realms----------------");
			for (Realm realm : realms) {
				try {
					realm.getSchema(realm);
					logger.info("Serving realm " + realm);
				} catch (SchemaLoadingException e) {
					logger.fatal("Can't serve realm " + realm, e);
				}
			}
		} catch (DirectoryException e) {
			logger.fatal("Can't init directory", e);
			throw e;
		}
	}

	/*
	 * @see org.openthinclient.tftp.tftpd.TFTPProvider#setOptions(java.util.Map)
	 */
	public void setOptions(Map<String, String> options) {
		if (!options.containsKey("template"))
			throw new IllegalArgumentException("Need the 'template' option");

		try {
			this.templateURL = new URL(options.get("template"));
		} catch (MalformedURLException e) {
			try {
				// try fallback to file syntax
				this.templateURL = new File(options.get("template")).toURL();
			} catch (MalformedURLException f) {
				throw new IllegalArgumentException(
						"template' option must contain a valid URL", f);
			}
		}
	}

	/*
	 * @see org.openthinclient.tftp.tftpd.TFTPProvider#getLength(java.lang.String,
	 *      java.lang.String)
	 */
	public long getLength(SocketAddress peer, SocketAddress local, String arg0,
			String arg1) throws IOException {
		return -1; // let the server determine the length
	}

	/*
	 * @see org.openthinclient.tftp.tftpd.TFTPProvider#getStream(java.lang.String,
	 *      java.lang.String)
	 */
	public InputStream getStream(SocketAddress peer, SocketAddress local,
			String prefix, String fileName) throws IOException {
		logger.info("Got request for " + fileName);

		if (fileName.contains("/") || fileName.length() != 20)
			throw new FileNotFoundException(
					"Don't know what to make of this file name: " + fileName);

		/*
		 * request will be 01-aa-bb-cc-dd-ee-ff -> mac address should be
		 * aa:bb:cc:dd:ee:ff
		 */
		String hwAddress = /*
												 * Ignore the media type for now.
												 * Integer.valueOf(fileName.substring(0, 2)) + "/" +
												 */
		fileName.substring(3).replaceAll("-", ":");

		logger.info("MAC is " + fileName);

		try {
			Client client = findClient(hwAddress);

			if (client != null) {
				logger.info("Serving Client " + client);
				String file = streamAsString(templateURL.openStream());

				if (logger.isDebugEnabled())
					logger.debug("Template: " + file);

				// initialize the global variables
				Map<String, String> globalVariables = new HashMap<String, String>();
				globalVariables.put("myip", ((InetSocketAddress) local).getAddress()
						.getHostAddress());
				globalVariables.put("basedn", client.getRealm()
						.getConnectionDescriptor().getBaseDN());

				String processed = resolveVariables(file, client, globalVariables);

				if (logger.isDebugEnabled())
					logger.debug("Processed template: >>>>\n" + processed + "<<<<\n");

				// kill \r's
				processed = processed.replaceAll("\\r", "");

				// join continuation lines
				processed = processed.replaceAll("\\\\[\\t ]*\\n", "");

				// save space by collapsing all spaces
				processed = processed.replaceAll("[\\t ]+", " ");

				if (logger.isDebugEnabled())
					logger.debug("Template after cleanup: >>>>\n" + processed + "<<<<\n");

				return new ByteArrayInputStream(processed.getBytes("ASCII"));
			}
		} catch (Exception e) {
			logger.error("Can't query for client for PXE service", e);
			new FileNotFoundException("Can't query for client for PXE service: " + e);
		}

		throw new FileNotFoundException("Client " + fileName + " not Found");
	}

	// pattern used to fill the template
	private static final Pattern TEMPLATE_REPLACEMENT_PATTERN = Pattern
			.compile("\\$\\{([^\\}]+)\\}");

	/**
	 * @param template
	 * @param client
	 * @param globalVariables
	 * @return
	 */
	private String resolveVariables(String template, Client client,
			Map<String, String> globalVariables) {
		StringBuffer result = new StringBuffer();
		Matcher m = TEMPLATE_REPLACEMENT_PATTERN.matcher(template);
		while (m.find()) {
			String variable = m.group(1);
			String encoding = "";

			if (variable.contains(":")) {
				encoding = variable.substring(0, variable.indexOf(":"));
				variable = variable.substring(variable.indexOf(":") + 1);
			}

			String value = client.getValue(variable);
			if (null == value) {
				value = globalVariables.get(variable);
				if (null == value)
					logger.warn("Pattern refers to undefined variable " + variable);
			}

			// resolve recursively
			if (null != value)
				value = resolveVariables(value, client, globalVariables);
			else
				value = "";

			// encode value: urlencoded,
			try {
				if (encoding.equalsIgnoreCase("base64")) {
					value = new String(Base64.encode(value.getBytes("UTF-8")));
				} else if (encoding.equalsIgnoreCase("urlencoded")) {
					value = URLEncoder.encode(value, "UTF-8");
				} else if (encoding.length() > 0) {
					logger.warn("Ignoring unsupported encoding: " + encoding);
				}
			} catch (UnsupportedEncodingException e) {
				// should never happen
				logger.error("That's silly: UTF8-encoding is unsupported!");
			}
			m.appendReplacement(result, value);
		}
		m.appendTail(result);

		String processed = result.toString();
		return processed;
	}

	/**
	 * @param hwAddress
	 * @return
	 * @throws DirectoryException
	 * @throws SchemaLoadingException
	 */
	private Client findClient(String hwAddress) throws DirectoryException,
			SchemaLoadingException {
		Client client = null;
		for (Realm r : realms) {
			Set<Client> found = r.getDirectory().list(Client.class, "",
					new Filter("(&(macAddress={0})(l=*))", hwAddress),
					TypeMapping.SearchScope.SUBTREE);

			if (found.size() > 0) {
				if (found.size() > 1)
					logger.warn("Found more than one client for hardware address "
							+ hwAddress);

				client = found.iterator().next();
				client.initSchemas(r);
				break;
			}
		}
		return client;
	}

	private String streamAsString(InputStream is) throws IOException {
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		byte b[] = new byte[1024];
		int read;
		while ((read = is.read(b)) >= 0)
			s.write(b, 0, read);

		is.close();
		return s.toString("ASCII");
	}
}
