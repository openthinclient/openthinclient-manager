package org.openthinclient.dhcp;

import java.net.InetSocketAddress;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.MessageType;
import org.apache.directory.server.dhcp.options.AddressOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.ServerIdentifier;
import org.apache.directory.server.dhcp.options.vendor.RootPath;
import org.apache.log4j.Logger;
import org.openthinclient.common.model.Client;
import org.openthinclient.ldap.DirectoryException;

/**
 * An abstract basic implementation of the PXE service for implementations which
 * send the PXE offers during the DISCOVER phase.
 * 
 * @author levigo
 */
public abstract class BasePXEService extends AbstractPXEService {

	protected static final Logger logger = Logger
			.getLogger(BindToAddressPXEService.class);

	public BasePXEService() throws DirectoryException {
		super();
	}

	/*
	 * @see org.apache.directory.server.dhcp.service.AbstractDhcpService#handleDISCOVER(java.net.InetSocketAddress,
	 *      org.apache.directory.server.dhcp.messages.DhcpMessage)
	 */
	@Override
	protected DhcpMessage handleDISCOVER(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage request)
			throws DhcpException {
		// this is a good time for some house-keeping
		expireConversations();

		if (!assertCorrectPort(localAddress, 67, request))
			return null;

		// detect PXE client
		if (!isPXEClient(request)) {
			if (logger.isInfoEnabled())
				logger.info("Ignoring non-PXE DISCOVER"
						+ getLogDetail(localAddress, clientAddress, request));
			return null;
		}

		// On some systems, e.g. MS Windows, broadcast messages may be delivered
		// several times, in cases where a physical interface has multiple
		// addresses. Skip those dupes.
		final RequestID requestID = new RequestID(request);
		if (conversations.containsKey(requestID)) {
			if (logger.isInfoEnabled())
				logger.info("Skipping duplicate DISCOVER for "
						+ conversations.get(requestID));
			return null;
		}

		if (logger.isInfoEnabled())
			logger.info("Got PXE DISCOVER"
					+ getLogDetail(localAddress, clientAddress, request));

		// Create conversation and immediately synchronize on it, in order to
		// prevent race conditions with other phases.
		final Conversation conversation = new Conversation(request);
		synchronized (conversation) {
			conversations.put(requestID, conversation);

			// check whether client is eligible for PXE service
			final String hwAddressString = request.getHardwareAddress()
					.getNativeRepresentation();
			final Client client = getClient(hwAddressString, clientAddress, request);

			// we create a conversation, even if the client was NOT found, since this
			// will allow us to track unrecognized PXE clients
			if (client == null) {
				logger.info("Client not eligible for PXE proxy service");

				trackUnrecognizedClient(request, null, null);
				return null;
			}

			conversation.setClient(client);

			logger.info("Conversation started");

			// answer DISCOVER messages
			return myHandleOFFER(localAddress, clientAddress, request);
		}
	}

	protected DhcpMessage myHandleOFFER(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage offer) throws DhcpException {

		final RequestID id = new RequestID(offer);
		final Conversation conversation = conversations.get(id);

		if (null == conversation) {
			// FIXME: reduce to debug once tested - this happens all the time
			// for non-PXE conversations
			if (logger.isInfoEnabled())
				logger.info("Got OFFER for which there is no conversation"
						+ getLogDetail(localAddress, clientAddress, offer));
			return null;
		}

		// synchronize on conversation to give any in-progress DISCOVERs
		// time to set the client
		synchronized (conversation) {
			if (conversation.isExpired()) {
				if (logger.isInfoEnabled())
					logger.info("Got OFFER for an expired conversation: " + conversation);
				conversations.remove(id);
				return null;
			}

			if (logger.isInfoEnabled())
				logger.info("Got OFFER within " + conversation);

			// we may need this later
			final InetSocketAddress serverAddress = determineServerAddress(localAddress);

			conversation.setApplicableServerAddress(serverAddress);

			// prepare PXE proxy offer
			final DhcpMessage reply = initGeneralReply(serverAddress, offer);

			reply.setMessageType(MessageType.DHCPOFFER);

			if (logger.isInfoEnabled())
				logger.info("Sending PXE proxy offer " + offer);

			return reply;

		}
	}

	/**
	 * Determine the server address to use.
	 * 
	 * @param localAddress the address of the socket which received the request.
	 * @return
	 */
	protected abstract InetSocketAddress determineServerAddress(
			InetSocketAddress localAddress);

	@Override
	protected DhcpMessage handleREQUEST(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage request)
			throws DhcpException {
		// detect PXE client
		if (!isPXEClient(request)) {
			if (logger.isInfoEnabled())
				logger.info("Ignoring non-PXE REQUEST"
						+ getLogDetail(localAddress, clientAddress, request));
			return null;
		}

		if (logger.isInfoEnabled())
			logger.info("Got PXE REQUEST"
					+ getLogDetail(localAddress, clientAddress, request));

		// clientAdress must be set
		if (isZeroAddress(clientAddress.getAddress())) {
			if (logger.isInfoEnabled())
				logger.info("Ignoring PXE REQUEST from 0.0.0.0"
						+ getLogDetail(localAddress, clientAddress, request));
			return null;
		}

		// we don't react to requests here, unless they go to port 4011
		if (!assertCorrectPort(localAddress, 4011, request))
			return null;

		// find conversation
		final RequestID id = new RequestID(request);
		final Conversation conversation = conversations.get(id);

		if (null == conversation) {
			if (logger.isInfoEnabled())
				logger.info("Got PXE REQUEST for which there is no conversation"
						+ getLogDetail(localAddress, clientAddress, request));
			return null;
		}

		synchronized (conversation) {
			if (conversation.isExpired()) {
				if (logger.isInfoEnabled())
					logger.info("Got PXE REQUEST for an expired conversation: "
							+ conversation);
				conversations.remove(id);
				return null;
			}

			final Client client = conversation.getClient();
			if (null == client) {
				logger.warn("Got PXE request which we didn't send an offer. "
						+ "Someone else is serving PXE around here?");
				return null;
			}

			if (logger.isDebugEnabled())
				logger.debug("Got PXE REQUEST within " + conversation);

			// check server ident
			final AddressOption serverIdentOption = (AddressOption) request
					.getOptions().get(ServerIdentifier.class);
			if (null != serverIdentOption
					&& serverIdentOption.getAddress().isAnyLocalAddress()) {
				if (logger.isInfoEnabled())
					logger.info("Ignoring PXE REQUEST for server " + serverIdentOption);
				return null; // not me!
			}

			final InetSocketAddress serverAddress = conversation
					.getApplicableServerAddress();
			final DhcpMessage reply = initGeneralReply(serverAddress, request);

			reply.setMessageType(MessageType.DHCPACK);

			final OptionsField options = reply.getOptions();

			reply.setNextServerAddress(getNextServerAddress(
					"BootOptions.TFTPBootserver", serverAddress, client));

			final String rootPath = getNextServerAddress("BootOptions.NFSRootserver",
					serverAddress, client).getHostAddress()
					+ ":" + client.getValue("BootOptions.NFSRootPath");
			options.add(new RootPath(rootPath));

			reply.setBootFileName(client.getValue("BootOptions.BootfileName"));

			if (logger.isInfoEnabled())
				logger
						.info("Sending PXE proxy ACK rootPath=" + rootPath
								+ " bootFileName=" + reply.getBootFileName()
								+ " nextServerAddress="
								+ reply.getNextServerAddress().getHostAddress() + " reply="
								+ reply);
			return reply;
		}
	}

}