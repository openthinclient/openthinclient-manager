package org.openthinclient.service.dhcp;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.ArchType;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.MessageType;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.VendorClassIdentifier;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.service.store.ClientBootData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * An abstract basic implementation of the PXE service for implementations which
 * send the PXE offers during the DISCOVER phase.
 * 
 * @author levigo
 */
public abstract class BasePXEService extends AbstractPXEService {

	protected static final Logger logger = LoggerFactory
					.getLogger(BasePXEService.class);

	/*
	 * @see
	 * org.apache.directory.server.dhcp.service.AbstractDhcpService#handleDISCOVER
	 * (java.net.InetSocketAddress,
	 * org.apache.directory.server.dhcp.messages.DhcpMessage)
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
		ArchType archType = ArchType.fromMessage(request);
		if (!archType.isPXEClient()) {
			if (logger.isDebugEnabled())
				logger.debug("Ignoring non-PXE DISCOVER"
						+ getLogDetail(localAddress, clientAddress, request));
			return null;
		}

		// On some systems, e.g. MS Windows, broadcast messages may be delivered
		// several times, in cases where a physical interface has multiple
		// addresses. Skip those dupes.
		final RequestID requestID = new RequestID(request);
		// if (conversations.containsKey(requestID)) {
		// if (logger.isInfoEnabled())
		// logger.info("Skipping duplicate DISCOVER for "
		// + conversations.get(requestID));
		// return null;
		// }

		if (logger.isInfoEnabled())
			logger.info("Got PXE DISCOVER"
					+ getLogDetail(localAddress, clientAddress, request));

		// Create conversation and immediately synchronize on it, in order to
		// prevent race conditions with other phases.
		final Conversation conversation = new Conversation(request, archType);
		synchronized (conversation) {
			conversations.put(requestID, conversation);

			// check whether client is eligible for PXE service
			final String hwAddressString = request.getHardwareAddress()
					.getNativeRepresentation();

			//final ClientBootData bootData = ClientBootData.load(hwAddressString);
			ClientBootData bootData = null;
			try {
				bootData = ClientBootData.load(hwAddressString);
			} catch (Exception ex) {
				logger.error("Could not load client data for {}", hwAddressString, ex);
				return null;
			}

			if (null == bootData) {
				logger.info("Client not eligible for PXE proxy service");

				trackUnrecognizedClient(request, null);
				return null;
			} else if (ClientService.DEFAULT_CLIENT_MAC.equals(bootData.getIP()))
				// also track "unrecognized" MAC if we are serving DEFAULT_CLIENT
				trackUnrecognizedClient(request, null);

			conversation.setClient(bootData);

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
			if (logger.isDebugEnabled())
				logger.debug("Got OFFER for which there is no conversation"
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
			final InetSocketAddress serverAddress = determineServerAddress(localAddress, offer);

			conversation.setApplicableServerAddress(serverAddress);

			// prepare PXE proxy offer
			final DhcpMessage reply = initGeneralReply(serverAddress, offer);

			reply.setMessageType(MessageType.DHCPOFFER);

			ArchType archType = conversation.getArchType();

			final OptionsField options = reply.getOptions();
			final VendorClassIdentifier vci = new VendorClassIdentifier();
			vci.setString(archType.isHTTP()? "HTTPClient" : "PXEClient");
			options.add(vci);

			if (archType.isHTTP()) {
				reply.setBootFileName(getBootFileURI(conversation));
			} else {
				final ClientBootData bootData = conversation.getBootData();
				if (null != bootData)
					reply.setNextServerAddress(
							getNextServerAddress(
									"BootOptions.TFTPBootserver",
									conversation.getApplicableServerAddress(),
									bootData));
			}
			if (logger.isInfoEnabled())
				logger.info("Sending PXE proxy offer " + offer);

			return reply;

		}
	}
}
