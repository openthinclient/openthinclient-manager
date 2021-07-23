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

import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.service.common.Service;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.tftp.tftpd.FilesystemProvider;
import org.openthinclient.tftp.tftpd.TFTPExport;
import org.openthinclient.tftp.tftpd.TFTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * @author levigo
 * @author jn
 */
public class TFTPService implements Service<TFTPServiceConfiguration> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TFTPService.class);

  /**
   * The default relative path to the exported tftp directory.
   */
  public static final Path DEFAULT_ROOT_PATH = Paths.get("nfs", "root", "tftp");

  private TFTPServer tftpServer;

  private TFTPServiceConfiguration configuration;

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private RealmService realmService;
  @Autowired
  private ClientService clientService;

  @Override
  public TFTPServiceConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public void setConfiguration(TFTPServiceConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public Class<TFTPServiceConfiguration> getConfigurationClass() {
    return TFTPServiceConfiguration.class;
  }

  public void startService() throws Exception {
    try {

      tftpServer = new TFTPServer(0 != configuration.getTftpPort() ? configuration.getTftpPort() : TFTPServer.DEFAULT_TFTP_PORT);

      for (TFTPServiceConfiguration.Export export : configuration.getExports()) {
        String prefix = export.getPrefix();
        String basedir = export.getBasedir();
        LOGGER.info("Exporting " + prefix + "=" + basedir);

        tftpServer.addExport(new TFTPExport(prefix, new FilesystemProvider(basedir)));
      }

      LOGGER.info("Exporting PXE Boot configuration");
      final Path tftpHome = managerHome.getLocation().toPath().resolve(DEFAULT_ROOT_PATH);
      tftpServer.addExport(new TFTPExport("/pxelinux.cfg", new PXEConfigTFTProvider(
              tftpHome,
              realmService,
              clientService,
              "template-http.txt",
              "template-tftp.txt")));
      tftpServer.addExport(new TFTPExport("/ipxe.cfg", new PXEConfigTFTProvider(
              tftpHome,
              realmService,
              clientService,
              "ipxe.cfg",
              "ipxe.cfg")));
      tftpServer.addExport(new TFTPExport("/localboot.cfg", new PXEConfigTFTProvider(
              tftpHome,
              realmService,
              clientService,
              "localboot.cfg",
              "localboot.cfg")));

      tftpServer.start();
      LOGGER.info("TFTP service launched");
    } catch (IOException e) {
      LOGGER.error("Exception launching TFTP service", e);
      throw e;
    }
  }

  public void stopService() throws Exception {
    if (null != tftpServer) {
      tftpServer.shutdown();
      LOGGER.info("TFTP service shut down");
    }
  }
}
