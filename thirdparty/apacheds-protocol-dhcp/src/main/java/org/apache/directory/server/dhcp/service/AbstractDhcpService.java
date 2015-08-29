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

package org.apache.directory.server.dhcp.service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Iterator;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.MessageType;
import org.apache.directory.server.dhcp.options.DhcpOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.ParameterRequestList;
import org.apache.directory.server.dhcp.options.dhcp.ServerIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of the server-side DHCP protocol. This class just
 * provides some utility methods and dispatches server-bound messages to handler
 * methods which can be overridden to provide the functionality.
 * <p>
 * Client-bound messages and BOOTP messages are ignored.
 */
public abstract class AbstractDhcpService implements DhcpService {
	private static final Logger logger = LoggerFactory
			.getLogger(AbstractDhcpService.class);

	/*
	 * @see org.apache.directory.server.dhcp.DhcpService#getReplyFor(org.apache.directory.server.dhcp.messages.DhcpMessage)
	 */
	public final DhcpMessage getReplyFor(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage request)
			throws DhcpException {
		// ignore messages with an op != REQUEST/REPLY
		if (request.getOp() != DhcpMessage.OP_BOOTREQUEST
				&& request.getOp() != DhcpMessage.OP_BOOTREPLY)
			return null;

		// message type option MUST be set - we don't support plain BOOTP.
		if (null == request.getMessageType()) {
			logger.warn("Missing message type option - plain BOOTP not supported.");
			return null;
		}

		// dispatch based on the message type
		switch (request.getMessageType().getCode()){
			// client-to-server messages
			case MessageType.CODE_DHCPDISCOVER :
				return handleDISCOVER(localAddress, clientAddress, request);
			case MessageType.CODE_DHCPREQUEST :
				return handleREQUEST(localAddress, clientAddress, request);
			case MessageType.CODE_DHCPRELEASE :
				return handleRELEASE(localAddress, clientAddress, request);
			case MessageType.CODE_DHCPINFORM :
				return handleINFORM(localAddress, clientAddress, request);

			case MessageType.CODE_DHCPOFFER :
				return handleOFFER(localAddress, clientAddress, request);

				// server-to-client messages
			case MessageType.CODE_DHCPDECLINE :
			case MessageType.CODE_DHCPACK :
			case MessageType.CODE_DHCPNAK :
				return null; // just ignore them

			default :
				return handleUnknownMessage(clientAddress, request);
		}
	}

	/**
	 * Handle unknown DHCP message. The default implementation just logs and
	 * ignores it.
	 * 
	 * @param clientAddress
	 * @param request the request message
	 * @return response message or <code>null</code> to ignore (don't reply to)
	 *         it.
	 */
	protected DhcpMessage handleUnknownMessage(InetSocketAddress clientAddress,
			DhcpMessage request) {
		if (logger.isWarnEnabled())
			logger.warn("Got unknkown DHCP message: " + request + " from:  "
					+ clientAddress);
		return null;
	}

	/**
	 * Handle DHCPINFORM message. The default implementation just ignores it.
	 * 
	 * @param clientAddress
	 * @param clientAddress2
	 * @param request the request message
	 * @return response message or <code>null</code> to ignore (don't reply to)
	 *         it.
	 */
	protected DhcpMessage handleINFORM(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage request)
			throws DhcpException {
		if (logger.isDebugEnabled())
			logger.debug("Got INFORM message: " + request + " from:  "
					+ clientAddress);
		return null;
	}

	/**
	 * Handle DHCPRELEASE message. The default implementation just ignores it.
	 * 
	 * @param localAddress
	 * @param clientAddress
	 * @param request the request message
	 * @return response message or <code>null</code> to ignore (don't reply to)
	 *         it.
	 */
	protected DhcpMessage handleRELEASE(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage request)
			throws DhcpException {
		if (logger.isDebugEnabled())
			logger.debug("Got RELEASE message: " + request + " from:  "
					+ clientAddress);
		return null;
	}

	/**
	 * Handle DHCPREQUEST message. The default implementation just ignores it.
	 * 
	 * @param localAddress
	 * @param clientAddress
	 * @param request the request message
	 * @return response message or <code>null</code> to ignore (don't reply to)
	 *         it.
	 */
	protected DhcpMessage handleREQUEST(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage request)
			throws DhcpException {
		if (logger.isDebugEnabled())
			logger.debug("Got REQUEST message: " + request + " from:  "
					+ clientAddress);
		return null;
	}

	/**
	 * Handle DHCPDISCOVER message. The default implementation just ignores it.
	 * 
	 * @param localAddress
	 * @param clientAddress
	 * @param request the request message
	 * @return response message or <code>null</code> to ignore (don't reply to)
	 *         it.
	 * @throws DhcpException
	 */
	protected DhcpMessage handleDISCOVER(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage request)
			throws DhcpException {
		if (logger.isDebugEnabled())
			logger.debug("Got DISCOVER message: " + request + " from:  "
					+ clientAddress);
		return null;
	}

	/**
	 * Handle DHCPOFFER message. The default implementation just ignores it.
	 * 
	 * @param localAddress
	 * @param clientAddress
	 * @param request the request message
	 * @return response message or <code>null</code> to ignore (don't reply to)
	 *         it.
	 * @throws DhcpException
	 */
	protected DhcpMessage handleOFFER(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage request)
			throws DhcpException {
		if (logger.isDebugEnabled())
			logger
					.debug("Got OFFER message: " + request + " from:  " + clientAddress);
		return null;
	}

	/**
	 * Initialize a general DHCP reply message. Sets:
	 * <ul>
	 * <li>op=BOOTREPLY
	 * <li>htype, hlen, xid, flags, giaddr, chaddr like in request message
	 * <li>hops, secs to 0.
	 * <li>server hostname to the hostname appropriate for the interface the
	 * request was received on
	 * <li>the server identifier set to the address of the interface the request
	 * was received on
	 * </ul>
	 * 
	 * @param localAddress
	 * @param request
	 * @return
	 */
	protected final DhcpMessage initGeneralReply(InetSocketAddress localAddress,
			DhcpMessage request) {
		final DhcpMessage reply = new DhcpMessage();

		reply.setOp(DhcpMessage.OP_BOOTREPLY);

		reply.setHardwareAddress(request.getHardwareAddress());
		reply.setTransactionId(request.getTransactionId());
		reply.setFlags(request.getFlags());
		reply.setRelayAgentAddress(request.getRelayAgentAddress());

		// set server hostname
		reply.setServerHostname(localAddress.getHostName());

		// set server identifier based on the IF on which we received the packet
		reply.getOptions().add(new ServerIdentifier(localAddress.getAddress()));

		return reply;
	}

	/**
	 * Check if an address is the zero-address
	 * 
	 * @param addr
	 * @return
	 */
	private boolean isZeroAddress(byte[] addr) {
		for (int i = 0; i < addr.length; i++)
			if (addr[i] != 0)
				return false;
		return true;
	}

	/**
	 * Determine address on which to base selection. If the relay agent address is
	 * set, we use the relay agent's address, otherwise we use the address we
	 * received the request from.
	 * 
	 * @param clientAddress
	 * @param request
	 * @return
	 */
	protected final InetAddress determineSelectionBase(
			InetSocketAddress clientAddress, DhcpMessage request) {
		// FIXME: do we know
		// a) the interface address over which we received a message (!)
		// b) the client address (if specified)
		// c) the relay agent address?

		// if the relay agent address is set, we use it as the selection base
		if (!isZeroAddress(request.getRelayAgentAddress().getAddress()))
			return request.getRelayAgentAddress();

		return clientAddress.getAddress();
	}

	/**
	 * Strip options that the client doesn't want, if the ParameterRequestList
	 * option is present.
	 * 
	 * @param request
	 * @param options
	 */
	protected final void stripUnwantedOptions(DhcpMessage request,
			OptionsField options) {
		final ParameterRequestList prl = (ParameterRequestList) request
				.getOptions().get(ParameterRequestList.class);
		if (null != prl) {
			final byte[] list = prl.getData();
			for (final Iterator i = options.iterator(); i.hasNext();) {
				final DhcpOption o = (DhcpOption) i.next();
				for (int j = 0; j < list.length; j++)
					if (list[j] == o.getTag())
						continue;
				i.remove();
			}
		}
	}
}
