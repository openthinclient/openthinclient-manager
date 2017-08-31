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
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.common.model.spring.ProfilePropertySource;
import org.openthinclient.common.model.util.Config;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.tftp.tftpd.TFTPProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author grafvr
 */
public class PXEConfigTFTProvider implements TFTPProvider {

  // pattern used to fill the template
  public static final Pattern TEMPLATE_REPLACEMENT_PATTERN = Pattern
          .compile("\\$\\{([^\\}]+)\\}");
  private static final Logger LOGGER = LoggerFactory.getLogger(PXEConfigTFTProvider.class);
  private final RealmService realmService;
  private final ClientService clientService;
  private final Path managerHome;
  private final Path fallbackTemplatePath;

  public PXEConfigTFTProvider(Path managerHome, RealmService realmService, ClientService clientService, Path fallbackTemplatePath) throws DirectoryException {
    this.managerHome = managerHome;
    this.realmService = realmService;
    this.clientService = clientService;
    this.fallbackTemplatePath = fallbackTemplatePath;

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

        final String file;
        file = getTemplate(client);


        if (LOGGER.isDebugEnabled())
          LOGGER.debug("Template: " + file);

        // initialize the global variables
        final Map<String, Object> globalVariables = new HashMap<>();
        globalVariables.put("myip", ((InetSocketAddress) local).getAddress().getHostAddress());
        globalVariables.put("basedn", client.getRealm().getConnectionDescriptor().getBaseDN());

        final CompositePropertySource propertySource = new CompositePropertySource("composite");

        propertySource.addFirstPropertySource(new ProfilePropertySource<>(client));
        propertySource.addPropertySource(new MapPropertySource("global", globalVariables));

        String processed = resolveVariables(file, propertySource);

        if (LOGGER.isDebugEnabled())
          LOGGER.debug("Processed template: >>>>\n" + processed + "<<<<\n");
        processed = compressTemplate(processed);


        if (LOGGER.isDebugEnabled())
          LOGGER.debug("Template after cleanup: >>>>\n" + processed + "<<<<\n");

        return new ByteArrayInputStream(processed.getBytes("ASCII"));
      }
    } catch (final Exception e) {
      LOGGER.error("Can't query for client for PXE service", e);
    }

    throw new FileNotFoundException("Client " + fileName + " not Found");
  }

  /**
   * Determines and reads the template to be used for the specified client.
   * <p>
   * This method will lookup a template using two steps:
   * </p>
   * <ol>
   * <li>Check if the {@link Client} has the
   * {@link Config.BootOptions#BootLoaderTemplate BootOptions.BootLoaderTemplate} configuration
   * set. If so, it will try to resolve the template specified for the client.</li>
   * <li>If no template could be resolved for the {@link Client}, the default fallback template
   * will be used to serve the client.</li>
   * </ol>
   *
   * @param client the {@link Client} for which a template shall be loaded.
   * @return the template contents as a {@link String}
   * @throws IOException in case of any error trying to access the template file.
   */
  private String getTemplate(Client client) throws IOException {
    // Try to get the template path if specified by the configuration
    final String templatePathString = Config.BootOptions.BootLoaderTemplate.get(client);

    if (templatePathString != null && templatePathString.trim().length() > 0) {
      final Path templatePath = managerHome.resolve(templatePathString);

      // FIXME validate that the path is not outside the manager home directory!

      if (!Files.isRegularFile(templatePath)) {
        LOGGER.error("Boot template is not accessible: " + templatePath);
      } else {
        try (InputStream is = Files.newInputStream(templatePath)) {
          return streamAsString(is);
        }
      }
    }

    try (InputStream is = Files.newInputStream(fallbackTemplatePath)) {
      return streamAsString(is);
    }
  }

  /**
   * Creates a compressed version of the template by replacing non required newlines and spaces.
   *
   * @param processed the {@link #resolveVariables(String, PropertySource) preprocessed} template
   * @return a compacted version of the template
   */
  protected String compressTemplate(String processed) {
    // kill \r's
    processed = processed.replaceAll("\\r", "");

    // join continuation lines
    processed = processed.replaceAll("\\\\[\\t ]*\\n", "");

    // save space by collapsing all spaces
    processed = processed.replaceAll("[\\t ]+", " ");
    return processed;
  }

  /**
   * @param template       the {@link String} template for which all properties shall be resolved.
   * @param propertySource the {@link PropertySource} implementation that shall be used to lookup
   *                       the required properties.
   * @return a {@link String} with all properties resolved
   */
  protected String resolveVariables(String template, PropertySource<?> propertySource) {

    // FIXME think about replacing this by using the PropertySourcesPropertyResolver
    final StringBuffer result = new StringBuffer();
    final Matcher m = TEMPLATE_REPLACEMENT_PATTERN.matcher(template);
    while (m.find()) {
      String variable = m.group(1);
      String encoding = "";

      if (variable.contains(":")) {
        encoding = variable.substring(0, variable.indexOf(":"));
        variable = variable.substring(variable.indexOf(":") + 1);
      }

      String value = (String) propertySource.getProperty(variable);
      // resolve recursively
      if (null != value)
        value = resolveVariables(value, propertySource);
      else {
        LOGGER.warn("Pattern refers to undefined variable " + variable);
        value = "";
      }

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

    return result.toString();
  }

  /**
   * @param hwAddress
   * @return
   * @throws DirectoryException
   * @throws SchemaLoadingException
   */
  private Client findClient(String hwAddress) throws DirectoryException,
          SchemaLoadingException {

    Set<Client> found = clientService.findByHwAddress(hwAddress);

    if (found.size() > 0) {
      if (found.size() > 1)
        LOGGER.warn("Found more than one client for hardware address "
                + hwAddress);

      return found.iterator().next();
    }

    // the following section basically assumes that there will only be a single realm available.
    // in the new version of the manager this assumption should be true.
    final Realm realm = realmService.getDefaultRealm();
    realm.refresh();

    final Config.BootOptions.PXEServicePolicyType policy = Config.BootOptions.PXEServicePolicy.get(realm);
    if (policy == Config.BootOptions.PXEServicePolicyType.AnyClient) {

      return clientService.getDefaultClient();

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
