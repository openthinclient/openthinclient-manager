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
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.support.DatagramSessionConfigImpl;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.service.common.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * A Service used to initialize the TCAT server.
 *
 * @author levigo
 */
public class DHCPService implements Service<DhcpServiceConfiguration> {

  private static final Logger logger = LoggerFactory.getLogger(DHCPService.class);

  private final ClientService clientService;
  private final UnrecognizedClientService unrecognizedClientService;
  private IoAcceptor acceptor;
  private AbstractPXEService dhcpService;
  private IoAcceptorConfig config;
  private DhcpProtocolHandler handler;
  private DhcpServiceConfiguration configuration;


  public DHCPService(ClientService clientService, UnrecognizedClientService unrecognizedClientService) {
    this.clientService = clientService;
    this.unrecognizedClientService = unrecognizedClientService;
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

    dhcpService = createPXEService(config);

    dhcpService.setTrackUnrecognizedPXEClients(configuration.isTrackUnrecognizedPXEClients());
    dhcpService.setPolicy(configuration.getPxe().getPolicy());

    handler = new DhcpProtocolHandler(dhcpService);
    dhcpService.init(acceptor, handler, config);
  }

  /**
   * Determine the kind of PXE service to use.
   */
  private AbstractPXEService createPXEService(IoAcceptorConfig config) throws DirectoryException {

    switch (configuration.getPxe().getType()) {
      case BIND_TO_ADDRESS:
        return new BindToAddressPXEService(clientService, unrecognizedClientService);
      case EAVESDROPPING:
        return new EavesdroppingPXEService(clientService, unrecognizedClientService);
      case SINGLE_HOMED_BROADCAST:
        return new SingleHomedBroadcastPXEService(clientService, unrecognizedClientService);
      case SINGLE_HOMED:
        return new SingleHomedPXEService(clientService, unrecognizedClientService);
      case AUTO:
        // fall through
      default:
        return autodetectPXEService(config);
    }
  }

  private AbstractPXEService autodetectPXEService(IoAcceptorConfig config) throws DirectoryException {
    // go for auto-detection:
    // try to bind to port 68. If we are successful, we are probably best served
    // with the Eavesdropping implementation.
    logger.info("Auto-detecting the PXE service implementation to use");
    try {
      final String osName = System.getProperty("os.name", "");
      if (osName.startsWith("Windows")) {
        logger.info("This seems to be Windows - going for the IndividualBind implementation");
        return new BindToAddressPXEService(clientService, unrecognizedClientService);
      }
    } catch (final Exception e) {
      logger.info("Can't use BindToAddress implementation");
      logger.info("Falling back to the SingleHomed implementation");

      return new SingleHomedPXEService(clientService, unrecognizedClientService);
    }

    try {
      return new SingleHomedBroadcastPXEService(clientService, unrecognizedClientService);
    } catch (final Exception e) {
      logger.info("Can't use SingleHomedBroadcastPXEService implementation");

      // try native implementation here, once we have it.
      logger.info("Falling back to the SingleHomed implementation");

      return new SingleHomedPXEService(clientService, unrecognizedClientService);
    }
  }

  @Override
  public void stopService() throws Exception {
    logger.info("Stopping...");
    if (null != acceptor)
      acceptor.unbindAll();
    acceptor = null;
    config = null;
    dhcpService = null;
    handler = null;
  }

}
