/*
 * Copyright 2005 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

package org.apache.directory.server.dhcp.protocol;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.MessageType;
import org.apache.directory.server.dhcp.service.DhcpService;
import org.apache.mina.common.BroadcastIoSession;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a DHCP protocol handler which delegates the work of
 * generating replys to a DhcpService implementation.
 * 
 * @see org.apache.directory.server.dhcp.service.DhcpService
 */
public class DhcpProtocolHandler implements IoHandler {
	private static final Logger logger = LoggerFactory
			.getLogger(DhcpProtocolHandler.class);

	/**
	 * Default DHCP client port
	 */
	public static final int CLIENT_PORT = 68;

	/**
	 * Default DHCP server port
	 */
	public static final int SERVER_PORT = 67;

	/**
	 * The DHCP service implementation. The implementation is supposed to be
	 * thread-safe.
	 */
	private final DhcpService dhcpService;

	/**
	 * 
	 */
	public DhcpProtocolHandler(DhcpService service) {
		this.dhcpService = service;
	}

	public void sessionCreated(IoSession session) throws Exception {
		logger.debug("{} CREATED", session.getLocalAddress());
		session.getFilterChain().addFirst("codec",
				new ProtocolCodecFilter(new DhcpProtocolCodecFactory()));
	}

	public void sessionOpened(IoSession session) {
		logger.debug("{} -> {} OPENED", session.getRemoteAddress(), session
				.getLocalAddress());
	}

	public void sessionClosed(IoSession session) {
		logger.debug("{} -> {} CLOSED", session.getRemoteAddress(), session
				.getLocalAddress());
	}

	public void sessionIdle(IoSession session, IdleStatus status) {
		// ignore
	}

	public void exceptionCaught(IoSession session, Throwable cause) {
		logger.error("EXCEPTION CAUGHT ", cause);
		cause.printStackTrace(System.out);

		session.close();
	}

	public void messageReceived(IoSession session, Object message)
			throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("{} -> {} RCVD: {} " + message, session.getRemoteAddress(),
					session.getLocalAddress());

		final DhcpMessage request = (DhcpMessage) message;

		final DhcpMessage reply = dhcpService.getReplyFor(
				(InetSocketAddress) session.getServiceAddress(),
				(InetSocketAddress) session.getRemoteAddress(), request);

		if (null != reply) {
			final InetSocketAddress isa = determineMessageDestination(request, reply);
			((BroadcastIoSession) session).write(reply, isa);
		}
	}

	/**
	 * Determine where to send the message: <br>
	 * If the 'giaddr' field in a DHCP message from a client is non-zero, the
	 * server sends any return messages to the 'DHCP server' port on the BOOTP
	 * relay agent whose address appears in 'giaddr'. If the 'giaddr' field is
	 * zero and the 'ciaddr' field is nonzero, then the server unicasts DHCPOFFER
	 * and DHCPACK messages to the address in 'ciaddr'. If 'giaddr' is zero and
	 * 'ciaddr' is zero, and the broadcast bit is set, then the server broadcasts
	 * DHCPOFFER and DHCPACK messages to 0xffffffff. If the broadcast bit is not
	 * set and 'giaddr' is zero and 'ciaddr' is zero, then the server unicasts
	 * DHCPOFFER and DHCPACK messages to the client's hardware address and
	 * 'yiaddr' address. In all cases, when 'giaddr' is zero, the server
	 * broadcasts any DHCPNAK messages to 0xffffffff.
	 * 
	 * @param request
	 * @param reply
	 * @return
	 */
	private InetSocketAddress determineMessageDestination(DhcpMessage request,
			DhcpMessage reply) {

		final MessageType mt = reply.getMessageType();
		if (!isNullAddress(request.getRelayAgentAddress()))
			// send to agent, if received via agent.
			return new InetSocketAddress(request.getRelayAgentAddress(), SERVER_PORT);
		else if (null != mt && mt == MessageType.DHCPNAK)
			// force broadcast for DHCPNAKs
			return new InetSocketAddress("255.255.255.255", 68);
		else // not a NAK...
		if (!isNullAddress(request.getCurrentClientAddress()))
			// have a current address? unicast to it.
			return new InetSocketAddress(request.getCurrentClientAddress(),
					CLIENT_PORT);
		else
			return new InetSocketAddress("255.255.255.255", 68);
	}

	/**
	 * Determine, whether the given address ist actually the null address
	 * "0.0.0.0".
	 * 
	 * @param relayAgentAddress
	 * @return
	 */
	private boolean isNullAddress(InetAddress addr) {
		final byte a[] = addr.getAddress();
		for (int i = 0; i < a.length; i++)
			if (a[i] != 0)
				return false;
		return true;
	}

	public void messageSent(IoSession session, Object message) {
		if (logger.isDebugEnabled())
			logger.debug("{} -> {} SENT: " + message, session.getRemoteAddress(),
					session.getLocalAddress());
	}
}
