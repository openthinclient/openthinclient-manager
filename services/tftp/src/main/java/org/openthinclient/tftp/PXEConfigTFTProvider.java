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

package org.openthinclient.tftp;

import org.apache.directory.shared.ldap.util.Base64;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.provider.AbstractSchemaProvider;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.common.model.schema.provider.ServerLocalSchemaProvider;
import org.openthinclient.common.model.service.DefaultLDAPRealmService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.tftp.tftpd.TFTPProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * @author grafvr
 */
public class PXEConfigTFTProvider implements TFTPProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(PXEConfigTFTProvider.class);
  // pattern used to fill the template
  private static final Pattern TEMPLATE_REPLACEMENT_PATTERN = Pattern
          .compile("\\$\\{([^\\}]+)\\}");
  private final String DEFAULT_CLIENT_MAC = "00:00:00:00:00:00";
  private final Set<Realm> realms;
  private URL templateURL;

  public PXEConfigTFTProvider() throws DirectoryException {

    // FIXME DirectoryServicesConfiguration already does this. This is kind of a duplicate, as right now, the PXEConfigTFTProvider is far away from reaching the spring beanfactory
    final File homeDirectory = new ManagerHomeFactory().getManagerHomeDirectory();

    RealmService service = new DefaultLDAPRealmService(new ServerLocalSchemaProvider(
            homeDirectory.toPath().resolve("nfs").resolve("root").resolve(AbstractSchemaProvider.SCHEMA_PATH)
    ));

    try {
      realms = service.findAllRealms();
      LOGGER.info("----------------realms----------------");
      for (final Realm realm : realms)
        try {
          realm.getSchema(realm);
          LOGGER.info("Serving realm " + realm);
        } catch (final SchemaLoadingException e) {
          LOGGER.error("Can't serve realm " + realm, e);
        }
    } catch (final Exception e) {
      LOGGER.error("Can't init directory", e);
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
    } catch (final MalformedURLException e) {
      try {
        // try fallback to file syntax
        this.templateURL = new File(options.get("template")).toURL();
      } catch (final MalformedURLException f) {
        throw new IllegalArgumentException(
                "template' option must contain a valid URL", f);
      }
    }
  }

  /*
   * @see org.openthinclient.tftp.tftpd.TFTPProvider#getLength(java.lang.String,
   * java.lang.String)
   */
  public long getLength(SocketAddress peer, SocketAddress local, String arg0,
                        String arg1) throws IOException {
    return -1; // let the server determine the length
  }

  /*
   * @see org.openthinclient.tftp.tftpd.TFTPProvider#getStream(java.lang.String,
   * java.lang.String)
   */
  public InputStream getStream(SocketAddress peer, SocketAddress local,
                               String prefix, String fileName) throws IOException {
    LOGGER.info("Got request for " + fileName);

    if (fileName.contains("/") || fileName.length() != 20)
      throw new FileNotFoundException(
              "Don't know what to make of this file name: " + fileName);

		/*
     * request will be 01-aa-bb-cc-dd-ee-ff -> mac address should be
		 * aa:bb:cc:dd:ee:ff
		 */
    final String hwAddress = /*
                               * Ignore the media type for now.
															 * Integer.valueOf(fileName.substring(0, 2)) + "/"
															 * +
															 */
            fileName.substring(3).replaceAll("-", ":");

    LOGGER.info("MAC is " + fileName);

    try {
      final Client client = findClient(hwAddress);

      if (client != null) {
        LOGGER.info("Serving Client " + client);
        final String file = streamAsString(templateURL.openStream());

        if (LOGGER.isDebugEnabled())
          LOGGER.debug("Template: " + file);

        // initialize the global variables
        final Map<String, String> globalVariables = new HashMap<String, String>();
        globalVariables.put("myip", ((InetSocketAddress) local).getAddress()
                .getHostAddress());
        globalVariables.put("basedn", client.getRealm()
                .getConnectionDescriptor().getBaseDN());

        String processed = resolveVariables(file, client, globalVariables);

        if (LOGGER.isDebugEnabled())
          LOGGER.debug("Processed template: >>>>\n" + processed + "<<<<\n");

        // kill \r's
        processed = processed.replaceAll("\\r", "");

        // join continuation lines
        processed = processed.replaceAll("\\\\[\\t ]*\\n", "");

        // save space by collapsing all spaces
        processed = processed.replaceAll("[\\t ]+", " ");

        if (LOGGER.isDebugEnabled())
          LOGGER.debug("Template after cleanup: >>>>\n" + processed + "<<<<\n");

        return new ByteArrayInputStream(processed.getBytes("ASCII"));
      }
    } catch (final Exception e) {
      LOGGER.error("Can't query for client for PXE service", e);
      new FileNotFoundException("Can't query for client for PXE service: " + e);
    }

    throw new FileNotFoundException("Client " + fileName + " not Found");
  }

  /**
   * @param template
   * @param client
   * @param globalVariables
   * @return
   */
  private String resolveVariables(String template, Client client,
                                  Map<String, String> globalVariables) {
    final StringBuffer result = new StringBuffer();
    final Matcher m = TEMPLATE_REPLACEMENT_PATTERN.matcher(template);
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
          LOGGER.warn("Pattern refers to undefined variable " + variable);
      }

      // resolve recursively
      if (null != value)
        value = resolveVariables(value, client, globalVariables);
      else
        value = "";

      // encode value: urlencoded,
      try {
        if (encoding.equalsIgnoreCase("base64"))
          value = new String(Base64.encode(value.getBytes("UTF-8")));
        else if (encoding.equalsIgnoreCase("urlencoded"))
          // java.net.URLEncoder converts " " into "+" as per the HTML
          // specification for form URL encoding.
          // we want URL encoding (" " to "%20") as per the URL specification
          value = URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20");
        else if (encoding.length() > 0)
          LOGGER.warn("Ignoring unsupported encoding: " + encoding);
      } catch (final UnsupportedEncodingException e) {
        // should never happen
        LOGGER.error("That's silly: UTF8-encoding is unsupported!");
      }
      m.appendReplacement(result, value);
    }
    m.appendTail(result);

    final String processed = result.toString();
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

    for (final Realm realm : realms) {
      Set<Client> found = realm.getDirectory().list(Client.class,
              new Filter("(&(macAddress={0})(l=*))", hwAddress),
              TypeMapping.SearchScope.SUBTREE);

      if (found.size() > 0) {
        if (found.size() > 1)
          LOGGER.warn("Found more than one client for hardware address "
                  + hwAddress);

        client = found.iterator().next();
        client.initSchemas(realm);
        return client;
      } else if (found.size() == 0) {
        final String pxeServicePolicy = realm
                .getValue("BootOptions.PXEServicePolicy");
        if ("AnyClient".equals(pxeServicePolicy)) {
          found = realm.getDirectory().list(Client.class,
                  new Filter("(&(macAddress={0})(l=*))", DEFAULT_CLIENT_MAC),
                  TypeMapping.SearchScope.SUBTREE);
          if (found.size() > 0) {
            if (found.size() > 1)
              LOGGER
                      .warn("Found more than one client for default hardware address "
                              + DEFAULT_CLIENT_MAC);

            client = found.iterator().next();
            client.initSchemas(realm);

            return client;
          }
        }
      }
    }
    return null;
  }

  private String streamAsString(InputStream is) throws IOException {
    final ByteArrayOutputStream s = new ByteArrayOutputStream();
    final byte b[] = new byte[1024];
    int read;
    while ((read = is.read(b)) >= 0)
      s.write(b, 0, read);

    is.close();
    return s.toString("ASCII");
  }
}
