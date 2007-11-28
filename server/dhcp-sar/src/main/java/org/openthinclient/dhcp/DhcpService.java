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

import org.apache.directory.server.dhcp.protocol.DhcpProtocolHandler;
import org.apache.log4j.Logger;
import org.apache.mina.common.ExecutorThreadModel;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoAcceptorConfig;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.support.DatagramSessionConfigImpl;
import org.jboss.system.ServiceMBeanSupport;

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

	@Override
	public void startService() throws Exception {
		logger.info("Starting...");
		acceptor = new DatagramAcceptor();
		final IoAcceptorConfig config = new DatagramAcceptorConfig();

		((DatagramSessionConfigImpl) config.getSessionConfig()).setBroadcast(true);

		final ExecutorThreadModel threadModel = ExecutorThreadModel
				.getInstance("DHCP");
		threadModel.setExecutor(new ThreadPoolExecutor(5, 5, 60, TimeUnit.SECONDS,
				new LinkedBlockingQueue()));
		config.setThreadModel(threadModel);

		// final DefaultIoFilterChainBuilder chain = config.getFilterChain();
		// chain.addLast("logger", new LoggingFilter());

		// PXE primer
		final AbstractPXEService dhcpService = new EavesdroppingPXEService();
		final DhcpProtocolHandler handler = new DhcpProtocolHandler(dhcpService);

		dhcpService.init(acceptor, handler, config);
	}

	@Override
	public void stopService() throws Exception {
		logger.info("Stopping...");
		if (null != acceptor)
			acceptor.unbindAll();
		acceptor = null;
	}
}
