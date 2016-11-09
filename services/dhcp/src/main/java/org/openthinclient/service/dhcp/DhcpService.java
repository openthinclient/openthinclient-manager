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
package org.openthinclient.service.dhcp;

import org.apache.directory.server.dhcp.protocol.DhcpProtocolHandler;
import org.apache.mina.common.ExecutorThreadModel;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.support.DatagramSessionConfigImpl;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.schema.provider.ServerLocalSchemaProvider;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DefaultLDAPClientService;
import org.openthinclient.common.model.service.DefaultLDAPRealmService;
import org.openthinclient.common.model.service.DefaultLDAPUnrecognizedClientService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.service.common.Service;
import org.openthinclient.services.Dhcp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * A Service used to initialize the TCAT server.
 *
 * @author levigo
 */
public class DhcpService implements Service<DhcpServiceConfiguration>, Dhcp {

  private static final Logger logger = LoggerFactory.getLogger(DhcpService.class);

  private final SchemaProvider schemaProvider;
  private final ClientService clientService;
  private final UnrecognizedClientService unrecognizedClientService;
  private final RealmService realmService;
  private IoAcceptor acceptor;
  private AbstractPXEService dhcpService;
  private IoAcceptorConfig config;
  private DhcpProtocolHandler handler;
  private DhcpServiceConfiguration configuration;

  public DhcpService() {

    schemaProvider = new ServerLocalSchemaProvider();
    realmService = new DefaultLDAPRealmService(schemaProvider);
    clientService = new DefaultLDAPClientService(realmService);
    unrecognizedClientService = new DefaultLDAPUnrecognizedClientService(realmService);
  }

  @Override
  public DhcpServiceConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public void setConfiguration(DhcpServiceConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public Class<DhcpServiceConfiguration> getConfigurationClass() {
    return DhcpServiceConfiguration.class;
  }

  @Override
  public void startService() throws Exception {
    logger.info("Starting...");
    acceptor = new DatagramAcceptor();
    config = new DatagramAcceptorConfig();
    ((DatagramSessionConfigImpl) config.getSessionConfig()).setReuseAddress(true);
    ((DatagramSessionConfigImpl) config.getSessionConfig()).setBroadcast(true);

    final ExecutorThreadModel threadModel = ExecutorThreadModel.getInstance("DHCP");
    threadModel.setExecutor(new ThreadPoolExecutor(5, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue()));
    config.setThreadModel(threadModel);

    dhcpService = createPXEService(acceptor, config);

    dhcpService.setTrackUnrecognizedPXEClients(configuration.isTrackUnrecognizedPXEClients());
    dhcpService.setPolicy(configuration.getPxe().getPolicy());

    handler = new DhcpProtocolHandler(dhcpService);
    dhcpService.init(acceptor, handler, config);
  }

  /**
   * Determine the kind of PXE service to use.
   */
  private AbstractPXEService createPXEService(IoAcceptor acceptor, IoAcceptorConfig config) throws DirectoryException {

    if (configuration.getPxe().getType() == DhcpServiceConfiguration.PXEType.BIND_TO_ADDRESS)
      return new BindToAddressPXEService(realmService, clientService, unrecognizedClientService, schemaProvider);
    else if (configuration.getPxe().getType() == DhcpServiceConfiguration.PXEType.EAVESDROPPING)
      return new EavesdroppingPXEService(realmService, clientService, unrecognizedClientService, schemaProvider);
    else if (configuration.getPxe().getType() == DhcpServiceConfiguration.PXEType.SINGLE_HOMED_BROADCAST)
      return new SingleHomedBroadcastPXEService(realmService, clientService, unrecognizedClientService, schemaProvider);
    else if (configuration.getPxe().getType() == DhcpServiceConfiguration.PXEType.SINGLE_HOMED)
      return new SingleHomedPXEService(realmService, clientService, unrecognizedClientService, schemaProvider);


    // go for auto-detection:
    // try to bind to port 68. If we are successful, we are probably best served
    // with the Eavesdropping implementation.
    logger.info("Auto-detecting the PXE service implementation to use");
    try {
      final String osName = System.getProperty("os.name", "");
      if (osName.startsWith("Windows")) {
        logger.info("This seems to be Windows - going for the IndividualBind implementation");
        return new BindToAddressPXEService(realmService, clientService, unrecognizedClientService, schemaProvider);
      }
    } catch (final Exception e) {
      logger.info("Can't use BindToAddress implementation");
      logger.info("Falling back to the SingleHomed implementation");

      return new SingleHomedPXEService(realmService, clientService, unrecognizedClientService, schemaProvider);
    }

    try {
      final InetSocketAddress dhcpClient = new InetSocketAddress(68);
      acceptor.bind(dhcpClient, new IoHandlerAdapter(), config);
      acceptor.unbind(dhcpClient);

      return new EavesdroppingPXEService(realmService, clientService, unrecognizedClientService, schemaProvider);
    } catch (final IOException e) {
      // nope, that one was already taken.
      logger
              .info("Can't use Eavesdropping implementation, bind to port 68 failed");

      // try native implementation here, once we have it.
      logger.info("Falling back to the SingleHomed implementation");

      return new SingleHomedPXEService(realmService, clientService, unrecognizedClientService, schemaProvider);
    }
  }

  @Override
  public void stopService() throws Exception {
    logger.info("Stopping...");
    if (null != acceptor)
      acceptor.unbindAll();
    acceptor = null;
  }

  public boolean reloadRealms() throws DirectoryException {
    realmService.reload();
    return true;
  }

}
