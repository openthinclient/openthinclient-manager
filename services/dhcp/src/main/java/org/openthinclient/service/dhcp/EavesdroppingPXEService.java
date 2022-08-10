package org.openthinclient.service.dhcp;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.ArchType;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.MessageType;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.VendorClassIdentifier;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.openthinclient.service.store.ClientBootData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * This PXE service implementation works passively, by "eavesdropping" on DHCP
 * OFFERs by a DHCP server. If an address is offered to a PXE enabled client, an
 * additional PXE offer is sent.
 * 
 * This is only possible by binding to port 68, the DHCP client port, which is
 * already occupied, if the host runs a DHCP client. Therefore this service
 * implementation cannot be used on hosts with dynamically configured addresses.
 * On MS Windows, it is alos necessary to disable the "DHCP client" service from
 * the control panel.
 * 
 * @author levigo
 */
public class EavesdroppingPXEService extends AbstractPXEService {
	private static final Logger logger = LoggerFactory
			.getLogger(EavesdroppingPXEService.class);

  protected InetSocketAddress determineServerAddress(
			InetSocketAddress localAddress, DhcpMessage message) {
		try {
      // determine server interface address to use
      InetAddress ifAddress = null;
      final InetAddress ca = message.getAssignedClientAddress();
      final byte assignedAddressBytes[] = ca.getAddress();
      outer : for (final Enumeration e = NetworkInterface
          .getNetworkInterfaces(); e.hasMoreElements();) {
        final NetworkInterface nif = (NetworkInterface) e.nextElement();
        for (final InterfaceAddress ia : nif.getInterfaceAddresses())
          if (isInSubnet(assignedAddressBytes,
              ia.getAddress().getAddress(), ia.getNetworkPrefixLength())) {
            ifAddress = ia.getAddress();
            break outer;
          }
      }
      if(null != ifAddress) {
        return new InetSocketAddress(ifAddress, 67);
      }
    } catch (final SocketException e) {
      logger.error("Error while determining network interface.", e);
    }
    return null;
  }

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

		// since the DHCP server is inherently multi-threaded, we have to do
		// something about cases where the DISCOVER phase is still in org.openthinclient.progress when
		// the OFFER from another server comes in. We do this, by creating a
		// conversation early and synchronizing on it, so that the latency-inducing
		// getClient() call doesn't lead to us to jumping to wrong conclusions while
		// handling the OFFER.
		final Conversation conversation = new Conversation(request, archType);
		synchronized (conversation) {
			conversations.put(requestID, conversation);

			// check whether client is eligible for PXE service
			final String hwAddressString = request.getHardwareAddress()
					.getNativeRepresentation();
			final ClientBootData bootData = ClientBootData.load(hwAddressString);

			// we create a conversation, even if the client was NOT found, since this
			// will allow us to track unrecognized PXE clients
			if (bootData == null) {
				logger.info("Client not eligible for PXE proxy service");
				return null;
			}

			conversation.setClient(bootData);

			logger.info("Conversation started");

			// we never answer DISCOVER messages, but wait for the
			// OFFER from the real DHCP server instead.
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.directory.server.dhcp.service.AbstractDhcpService#handleOFFER
	 * (java.net.InetSocketAddress, java.net.InetSocketAddress,
	 * org.apache.directory.server.dhcp.messages.DhcpMessage)
	 */
	@Override
	protected DhcpMessage handleOFFER(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage offer) throws DhcpException {
		if (!assertCorrectPort(localAddress, 68, offer))
			return null;

		// ignore other PXE offers
		if (isZeroAddress(offer.getAssignedClientAddress())) {
			logger.debug("Ignoring PXE proxy offer "
					+ getLogDetail(localAddress, clientAddress, offer));
			return null;
		}

		final RequestID id = new RequestID(offer);
		final Conversation conversation = conversations.get(id);

		if (null == conversation) {
			if (logger.isDebugEnabled())
				logger.debug("Got OFFER for which there is no conversation"
						+ getLogDetail(localAddress, clientAddress, offer));
			return null;
		}

		// synchronize on conversation to give any in-org.openthinclient.progress DISCOVERs
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
			if (conversation.getBootData() == null) {
				trackUnrecognizedClient(conversation.getDiscover(),
						offer.getAssignedClientAddress().getHostAddress());
			} else {
			  InetSocketAddress applicableServerAddress = determineServerAddress(localAddress, offer);
			  if (applicableServerAddress != null) {
			    	// we'll need this later
					conversation.setApplicableServerAddress(applicableServerAddress);

					// prepare PXE proxy offer
					final DhcpMessage reply = initGeneralReply(applicableServerAddress,
							offer);
					reply.setMessageType(MessageType.DHCPOFFER);

					if (conversation.getArchType().isHTTP()) {
						final OptionsField options = reply.getOptions();
						final VendorClassIdentifier vci = new VendorClassIdentifier();
						vci.setString("HTTPClient");
						options.add(vci);
						reply.setBootFileName(getBootFileURI(conversation));
					}

					if (logger.isInfoEnabled())
						logger.info("Sending PXE proxy offer " + offer);

					return reply;
			  } else {
			    logger.error("InterfaceAddress not found for " + offer + ", "
            + conversation);
			  }
			}
			return null;
		}
	}

	@Override
	public void init(IoAcceptor acceptor, IoHandler handler,
			IoServiceConfig config) throws IOException {
		logger.warn("-------------------------------------------------------------");
		logger.warn("  Using EavesdroppingPXEService implementation.");
		logger.warn("  This type of PXE service will additionally bind on");
		logger.warn("  port 68 (bootpc) to analyse DHCP-server messages as well.");
		logger.warn("  (for more details, see log messages with level INFO)");
		logger.info("");

		final InetSocketAddress dhcpCPort = new InetSocketAddress(67);
		logger.info("  Binding on " + dhcpCPort);
		acceptor.bind(dhcpCPort, handler, config);

		// yep, that's right, we listen for server messages as well!
		final InetSocketAddress dhcpSPort = new InetSocketAddress(68);
		logger.info("  Binding on " + dhcpSPort);
		acceptor.bind(dhcpSPort, handler, config);

		final InetSocketAddress pxePort = new InetSocketAddress(4011);
		logger.info("  Binding on " + pxePort);
		acceptor.bind(pxePort, handler, config);
		logger.warn("-------------------------------------------------------------");
	}
}
