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

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.directory.shared.ldap.util.Base64;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.service.store.ClientBootData;
import org.openthinclient.service.store.LDAPConnection;
import org.openthinclient.tftp.tftpd.TFTPProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * @author grafvr
 */
public class PXEConfigTFTProvider implements TFTPProvider {

  private static final Logger LOG =
      LoggerFactory.getLogger(PXEConfigTFTProvider.class);

  // pattern used to fill the template
  public static final Pattern TEMPLATE_REPLACEMENT_PATTERN =
      Pattern.compile("\\$\\{([^\\}]+)\\}");

  private final Path tftpHome;
  private final String fastTemplate;
  private final String safeTemplate;
  private final Cache<Path, String> templateCache;

  public PXEConfigTFTProvider(Path tftpHome,
                              String fastTemplate,
                              String safeTemplate)
  throws DirectoryException {
    this.tftpHome = tftpHome;
    this.fastTemplate = fastTemplate;
    this.safeTemplate = safeTemplate;

    templateCache = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.SECONDS)
        .build();
  }

  /*
   * @see org.openthinclient.tftp.tftpd.TFTPProvider#getLength(java.lang.String,
   * java.lang.String)
   */
  public long getLength(SocketAddress peer,
                        SocketAddress local,
                        String prefix,
                        String filename)
  throws IOException {
    return -1; // let the server determine the length
  }

  /*
   * @see org.openthinclient.tftp.tftpd.TFTPProvider#getStream(java.lang.String,
   * java.lang.String)
   */
  public InputStream getStream(SocketAddress peer,
                               SocketAddress local,
                               String prefix,
                               String fileName)
  throws IOException {
    LOG.debug("Got request for " + fileName);

    if (fileName.contains("/") || fileName.length() != 20)
      throw new FileNotFoundException(
              "Don't know what to make of this file name: " + fileName);

    final String hwAddress = fileName.substring(3).replaceAll("-", ":");
    String myip = ((InetSocketAddress) local).getAddress().getHostAddress();

    ClientBootData bootData;
    try {
      bootData = ClientBootData.load(hwAddress);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    if (bootData != null) {
      Path templatePath = getTemplatePath(bootData);
      String template = templateCache.get(templatePath, this::readFile);
      String config = resolveVariables(template, bootData, myip);
      return new ByteArrayInputStream(config.getBytes("ASCII"));
    } else {
      throw new FileNotFoundException("Client " + fileName + " not found");
    }
  }

  protected Path getTemplatePath(ClientBootData bootData) {
    String templatePathString =
        bootData.get("BootOptions.BootLoaderTemplate", null);
    if(templatePathString != null) {
      LOG.warn("Template path stored directly: {}", templatePathString);
    } else if("safe".equals(bootData.get("BootOptions.BootMode", null))) {
      templatePathString = safeTemplate;
    } else {
      templatePathString = fastTemplate;
    }
    return tftpHome.resolve(templatePathString);
  }

  private final static Pattern REMOVE = Pattern.compile("\\r|\\\\[\\t ]*\\n");
  private final static Pattern MULTI_WHITESPACE = Pattern.compile("[\\t ]+");
  private String readFile(Path path) /* throws IOException */ {
    try {
      String config = new String(Files.readAllBytes(path), "ASCII");
      // remove \r and line continuation
      config = REMOVE.matcher(config).replaceAll("");
      // save space by collapsing all spaces;
      config = MULTI_WHITESPACE.matcher(config).replaceAll(" ");
      return config;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  protected String resolveVariables(String template,
                                    ClientBootData bootData,
                                    String myip) {
    final StringBuffer result = new StringBuffer();

    final Matcher m = TEMPLATE_REPLACEMENT_PATTERN.matcher(template);
    while (m.find()) {
      String variable = m.group(1);
      String encoding = "";

      int i = variable.indexOf(":");
      if (i > 0) {
        encoding = variable.substring(0, i);
        variable = variable.substring(i + 1);
      }

      String value;
      if ("myip".equals(variable)) {
        value = myip;
      } else if ("basedn".equals(variable)) {
        value = LDAPConnection.BASE_DN;
      } else {
        value = bootData.get(variable, null);
      }
      if (value != null) {
        value = resolveVariables(value, bootData, myip);
      } else {
        LOG.warn("Pattern refers to undefined variable " + variable);
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
          LOG.warn("Ignoring unsupported encoding: " + encoding);
      } catch (final UnsupportedEncodingException e) {
        // should never happen
        LOG.error("That's silly: UTF8-encoding is unsupported!");
      }
      m.appendReplacement(result, value);
    }
    m.appendTail(result);

    return result.toString();
  }
}
