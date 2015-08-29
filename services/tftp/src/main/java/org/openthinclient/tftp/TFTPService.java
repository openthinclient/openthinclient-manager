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

import java.io.IOException;
import java.util.*;

import javax.xml.transform.TransformerException;

import org.openthinclient.service.common.Service;
import org.openthinclient.tftp.tftpd.TFTPExport;
import org.openthinclient.tftp.tftpd.TFTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author levigo
 * @author jn
 */
public class TFTPService implements Service<TFTPServiceConfiguration> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TFTPService.class);

  private TFTPServer tftpServer;

  /* Attribute Names */
/*  public static String ATTR_PREFIX = "prefix";
  public static String ATTR_BASEDIR = "basedir";

  public static String ATTR_PROVIDER_CLASS_NAME = "provider-class";*/

  private TFTPServiceConfiguration configuration;

  @Override
  public void setConfiguration(TFTPServiceConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public TFTPServiceConfiguration getConfiguration() {
      return configuration;
  }

  @Override
  public Class<TFTPServiceConfiguration> getConfigurationClass() {
      return TFTPServiceConfiguration.class;
  }

  /**
   * The service keeps a copy of the list of exports, so that it can maintain it
   * while the server itself is down.
   */
  private Set<TFTPExport> persistentExports = new HashSet<TFTPExport>();

  public void startService() throws Exception {
    try {
      for (TFTPServiceConfiguration.Export export : configuration.getExports()) {

          String prefix = export.getPrefix();
          Class<?> providerClass = null;
          if (export.getProviderClass() != null) {
              providerClass = export.getProviderClass();
          }
          String basedir = export.getBasedir();
          List<TFTPServiceConfiguration.Export.Option> options = export.getOptions();

          LOGGER.info("Exporting " + prefix + "="
                  + (null != providerClass ? providerClass : basedir)
                  + " options=" + options);

          if (null != providerClass) {
              Map<String, String> opts = new HashMap<String, String>();
              for (TFTPServiceConfiguration.Export.Option option : options) {
                  opts.put(option.getName(), option.getValue());
              }
              addExport(new TFTPExport(prefix, providerClass, opts));
            } else {
              addExport(new TFTPExport(prefix, basedir));
          }
        }

      tftpServer = new TFTPServer(0 != configuration.getTftpPort() ? configuration.getTftpPort() : TFTPServer.DEFAULT_TFTP_PORT);

      if (null != persistentExports)
        for (TFTPExport export : persistentExports)
          tftpServer.addExport(export);

      tftpServer.start();
      LOGGER.info("TFTP service launched");
    } catch (IOException e) {
      LOGGER.error("Exception launching TFTP service", e);
      throw e;
    }
  }

  public void stopService() throws Exception {
    if (null != tftpServer) {
      // save exports
      persistentExports.clear();
      persistentExports.addAll(tftpServer.getExports());

      tftpServer.shutdown();
      LOGGER.info("TFTP service shut down");
    }
  }

  public void addExport(TFTPExport export) {
    if (null != tftpServer)
      tftpServer.addExport(export);
    else
      persistentExports.add(export);
  }

  public void removeExport(TFTPExport export) {
    if (null != tftpServer)
      tftpServer.removeExport(export);
    else
      persistentExports.remove(export);
  }

  public Set<TFTPExport> getExports() {
    if (null != tftpServer)
      return tftpServer.getExports();
    else
      return persistentExports;
  }

/*  *//**
   * Wrapper for addExport, creating new TFTPExport-Objects, using the
   * configured TFTP-Xports
   * 
   * @throws ClassNotFoundException
   * @throws TransformerException
   *//*
  public void setExports(org.w3c.dom.Element root) {
    try {
      NodeIterator i = XPathAPI.selectNodeIterator(root, "/entries/tftpexport");
      Node export;
      while (null != (export = i.nextNode())) {
        *//* pre-(null)initialized entries *//*
        String prefix = safeGetAttribute(export, ATTR_PREFIX);
        String providerClassName = safeGetAttribute(export,
            ATTR_PROVIDER_CLASS_NAME);
        String basedir = safeGetAttribute(export, ATTR_BASEDIR);

        NodeIterator j = XPathAPI.selectNodeIterator(export, "option");
        Node option;
        Map<String, String> options = new HashMap<String, String>();
        while (null != (option = j.nextNode())) {
          String name = safeGetAttribute(option, "name");
          String value = option.getTextContent();
          if (null == name || null == value)
            throw new IllegalArgumentException(
                "INVALID Configuration: options element needs name attribute and value");
          options.put(name, value);
        }

        if (null == prefix)
          throw new IllegalArgumentException(
              "INVALID Configuration: attribute prefix is required");

        if (null == providerClassName && null == basedir)
          throw new IllegalArgumentException(
              "INVALID Configuration: either provider-class-name or basedir is required");

        LOGGER.info("Exporting " + prefix + "="
                + (null != providerClassName ? providerClassName : basedir)
                + " options=" + options);
        if (null != providerClassName)
          try {
            addExport(new TFTPExport(prefix, providerClassName, options));
          } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("The class " + providerClassName
                + " cannot be found", e);
          }
        else
          addExport(new TFTPExport(prefix, basedir));
      }
    } catch (TransformerException e) {
      throw new IllegalArgumentException(
          "Problem parsing the supplied export spec", e);
    }
  }*/

/*  *//**
   * @param n
   * @param name
   * @return
   *//*
  private String safeGetAttribute(Node n, String name) {
    Node attribute = n.getAttributes().getNamedItem(name);
    if (null == attribute)
      return null;

    return attribute.getNodeValue();
  }*/
}
