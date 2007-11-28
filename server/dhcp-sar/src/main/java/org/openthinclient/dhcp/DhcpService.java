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

import java.net.InetSocketAddress;

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

		// To properly serve DHCP, we must bind to all local addresses
		// individually, in order to be able to distinguish, from which network
		// (on a multi-homed machine) a broadcast came.
		// bind to all local ports. Gobble up all addresses we can find.

		// This is a Problem. See:
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4212324
		// for (Enumeration i = NetworkInterface.getNetworkInterfaces(); i
		// .hasMoreElements();) {
		// NetworkInterface nif = (NetworkInterface) i.nextElement();
		// for (Enumeration j = nif.getInetAddresses(); j.hasMoreElements();) {
		// InetAddress address = (InetAddress) j.nextElement();
		//
		// if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
		// // we bind to both the standard DHCP port AND the PXE port
		// final InetSocketAddress dhcpPort = new InetSocketAddress(address, 67);
		// acceptor.bind(dhcpPort, handler, config);
		// logger.info("Listening on " + dhcpPort);
		//
		// final InetSocketAddress pxePort = new InetSocketAddress(address, 4011);
		// acceptor.bind(pxePort, handler, config);
		// logger.info("Listening on " + pxePort);
		// }
		// }
		// }

		final InetSocketAddress dhcpCPort = new InetSocketAddress(67);
		acceptor.bind(dhcpCPort, handler, config);
		logger.info("Listening on " + dhcpCPort);

		// yep, that's right, we listen for server messages as well!
		final InetSocketAddress dhcpSPort = new InetSocketAddress(68);
		acceptor.bind(dhcpSPort, handler, config);
		logger.info("Listening on " + dhcpSPort);

		final InetSocketAddress pxePort = new InetSocketAddress(4011);
		acceptor.bind(pxePort, handler, config);
		logger.info("Listening on " + pxePort);
	}

	@Override
	public void stopService() throws Exception {
		logger.info("Stopping...");
		if (null != acceptor)
			acceptor.unbindAll();
		acceptor = null;
	}
}
