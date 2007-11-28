package org.openthinclient.dhcp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

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

public class IndividuallyBindungPXEService extends AbstractPXEService {
	private static final Logger logger = Logger
			.getLogger(IndividuallyBindungPXEService.class);

	public IndividuallyBindungPXEService() throws DirectoryException {
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

		// since the DHCP server is inherently multi-threaded, we have to do
		// something about cases where the DISCOVER phase is still in progress when
		// the OFFER from another server comes in. We do this, by creating a
		// conversation early and synchronizing on it, so that the latency-inducing
		// getClient() call doesn't lead to us to jumping to wrong conclusions while
		// handling the OFFER.
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
				return null;
			}

			conversation.setClient(client);

			logger.info("Conversation started");

			// we never answer DISCOVER messages, but wait for the
			// OFFER from the real DHCP server instead.
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.directory.server.dhcp.service.AbstractDhcpService#handleOFFER(java.net.InetSocketAddress,
	 *      java.net.InetSocketAddress,
	 *      org.apache.directory.server.dhcp.messages.DhcpMessage)
	 */
	@Override
	protected DhcpMessage handleOFFER(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage offer) throws DhcpException {
		if (!assertCorrectPort(localAddress, 68, offer))
			return null;

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

			// track unrecognized clients
			if (conversation.getClient() == null)
				trackUnrecognizedClient(conversation.getDiscover(), offer);
			else
				try {
					// determine server interface address to use
					final InetAddress ca = offer.getAssignedClientAddress();
					final NetworkInterface nif = NetworkInterface.getByInetAddress(ca);
					if (null == nif) {
						logger.error("Interface not found for " + offer + ", "
								+ conversation);
						return null;
					}

					final byte assignedAddressBytes[] = ca.getAddress();
					InetAddress ifAddress = null;
					for (final InterfaceAddress ia : nif.getInterfaceAddresses())
						if (isInSubnet(assignedAddressBytes, ia.getAddress().getAddress(),
								ia.getNetworkPrefixLength())) {
							ifAddress = ia.getAddress();
							break;
						}

					if (null == ifAddress) {
						logger.error("InterfaceAddress not found for " + offer + ", "
								+ conversation);
						return null;
					}

					final InetSocketAddress applicableServerAddress = new InetSocketAddress(
							ifAddress, 67);

					// we'll need this later
					conversation.setApplicableServerAddress(applicableServerAddress);

					// prepare PXE proxy offer
					final DhcpMessage reply = initGeneralReply(applicableServerAddress,
							offer);
					reply.setMessageType(MessageType.DHCPOFFER);

					if (logger.isInfoEnabled())
						logger.info("Sending PXE proxy offer " + offer);

					return reply;
				} catch (final SocketException e) {
					logger.error("Can't determine network interface for " + offer + ", "
							+ conversation, e);

					// fall out
				}

			return null;
		}
	}

	/*
	 * @see org.apache.directory.server.dhcp.service.AbstractDhcpService#handleREQUEST(java.net.InetSocketAddress,
	 *      org.apache.directory.server.dhcp.messages.DhcpMessage)
	 */
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

			final DhcpMessage reply = initGeneralReply(conversation
					.getApplicableServerAddress(), request);

			reply.setMessageType(MessageType.DHCPACK);

			final OptionsField options = reply.getOptions();

			reply.setNextServerAddress(getNextServerAddress(
					"BootOptions.TFTPBootserver", conversation
							.getApplicableServerAddress(), client));

			final String rootPath = getNextServerAddress("BootOptions.NFSRootserver",
					conversation.getApplicableServerAddress(), client).getHostAddress()
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
