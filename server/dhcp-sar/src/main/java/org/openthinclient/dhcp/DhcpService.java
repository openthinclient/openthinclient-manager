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
package org.openthinclient.dhcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

import org.apache.directory.server.dhcp.protocol.DhcpProtocolHandler;
import org.apache.log4j.Logger;
import org.apache.mina.common.ExecutorThreadModel;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.support.DatagramSessionConfigImpl;
import org.jboss.system.ServiceMBeanSupport;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * A Service used to initialize the TCAT server.
 * 
 * @author levigo
 */
public class DhcpService extends ServiceMBeanSupport
		implements
			DhcpServiceMBean {

	private static final Logger logger = Logger.getLogger(DhcpService.class);

	private IoAcceptor acceptor;

	private AbstractPXEService dhcpService;

	private IoAcceptorConfig config;

	private DhcpProtocolHandler handler;

	private Set<Realm> realms;

	private LDAPConnectionDescriptor lcd;

	@Override
	public void startService() throws Exception {
		logger.info("Starting...");
		acceptor = new DatagramAcceptor();
		config = new DatagramAcceptorConfig();
		((DatagramSessionConfigImpl) config.getSessionConfig())
				.setReuseAddress(true);
		((DatagramSessionConfigImpl) config.getSessionConfig()).setBroadcast(true);

		final ExecutorThreadModel threadModel = ExecutorThreadModel
				.getInstance("DHCP");
		threadModel.setExecutor(new ThreadPoolExecutor(5, 5, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue()));
		config.setThreadModel(threadModel);

		lcd = new LDAPConnectionDescriptor();
		lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
		lcd.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
		lcd.setCallbackHandler(new UsernamePasswordHandler("uid=admin,ou=system",
				System.getProperty("ContextSecurityCredentials", "secret")
						.toCharArray()));
		realms = LDAPDirectory.findAllRealms(lcd);

		dhcpService = createPXEService(acceptor, config);
		handler = new DhcpProtocolHandler(dhcpService);
		dhcpService.init(acceptor, handler, config);
	}

	/**
	 * Determine the kind of PXE service to use.
	 * 
	 * @param acceptor
	 * @param config
	 * 
	 * @return
	 * @throws DirectoryException
	 */
	private AbstractPXEService createPXEService(IoAcceptor acceptor,
			IoAcceptorConfig config) throws DirectoryException {

		if (realms.size() != 1)
			// should not happen right now
			logger.error("Can just handle one realm - going for auto-detection");
		else {
			final String configuredPxeService = realms.iterator().next()
					.getValue("BootOptions.PXEService");

			if ("BindToAddressPXEService".equals(configuredPxeService))
				return new BindToAddressPXEService();
			else if ("EavesdroppingPXEService".equals(configuredPxeService))
				return new EavesdroppingPXEService();
			else if ("SingleHomedBroadcastPXEService".equals(configuredPxeService))
				return new SingleHomedBroadcastPXEService();
			else if ("SingleHomedPXEService".equals(configuredPxeService))
				return new SingleHomedPXEService();
		}

		// go for auto-detection:
		// try to bind to port 68. If we are successful, we are probably best served
		// with the Eavesdropping implementation.
		logger.info("Auto-detecting the PXE service implementation to use");
		try {
			final String osName = System.getProperty("os.name", "");
			if (osName.startsWith("Windows")) {
				logger
						.info("This seems to be Windows - going for the IndividualBind implementation");
				return new BindToAddressPXEService();
			}
		} catch (final Exception e) {
			logger.info("Can't use BindToAddress implementation");
			logger.info("Falling back to the SingleHomed implementation");

			return new SingleHomedPXEService();
		}

		try {
			final InetSocketAddress dhcpClient = new InetSocketAddress(68);
			acceptor.bind(dhcpClient, new IoHandlerAdapter(), config);
			acceptor.unbind(dhcpClient);

			return new EavesdroppingPXEService();
		} catch (final IOException e) {
			// nope, that one was already taken.
			logger
					.info("Can't use Eavesdropping implementation, bind to port 68 failed");

			// try native implementation here, once we have it.
			logger.info("Falling back to the SingleHomed implementation");

			return new SingleHomedPXEService();
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
		return dhcpService.reloadRealms();
	}
}
