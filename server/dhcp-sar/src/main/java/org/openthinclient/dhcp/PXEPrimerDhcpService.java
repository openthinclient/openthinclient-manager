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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.messages.MessageType;
import org.apache.directory.server.dhcp.options.AddressOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.ServerIdentifier;
import org.apache.directory.server.dhcp.options.dhcp.VendorClassIdentifier;
import org.apache.directory.server.dhcp.options.vendor.HostName;
import org.apache.directory.server.dhcp.options.vendor.RootPath;
import org.apache.directory.server.dhcp.service.AbstractDhcpService;
import org.apache.log4j.Logger;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.common.model.schema.provider.ServerLocalSchemaProvider;
import org.openthinclient.common.util.UsernamePasswordHandler;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.TypeMapping;

/**
 * @author levigo
 */
public class PXEPrimerDhcpService extends AbstractDhcpService {
	/**
	 * Key object used to index conversations.
	 */
	private static final class RequestID {
		private final HardwareAddress mac;
		private final int transactionID;

		public RequestID(DhcpMessage m) {
			this.mac = m.getHardwareAddress();
			this.transactionID = m.getTransactionId();
		}

		@Override
		public boolean equals(Object obj) {
			return obj != null // 
					&& obj.getClass().equals(getClass())
					&& transactionID == ((RequestID) obj).transactionID
					&& mac.equals(((RequestID) obj).mac);
		}

		@Override
		public int hashCode() {
			return 834532 ^ transactionID ^ mac.hashCode();
		}
	}

	/**
	 * Conversation models a DHCP conversation from DISCOVER through REQUEST.
	 */
	private static final class Conversation {
		private static final int CONVERSATION_EXPIRY = 5000;
		private final DhcpMessage discover;
		private Client client;
		private DhcpMessage offer;
		private long lastAccess;
		private InetSocketAddress applicableServerAddress;

		public Conversation(DhcpMessage discover) {
			this.discover = discover;
			touch();
		}

		private void touch() {
			this.lastAccess = System.currentTimeMillis();
		}

		public boolean isExpired() {
			return lastAccess < System.currentTimeMillis() - CONVERSATION_EXPIRY;
		}

		public DhcpMessage getOffer() {
			touch();
			return offer;
		}

		public void setOffer(DhcpMessage offer) {
			touch();
			this.offer = offer;
		}

		public DhcpMessage getDiscover() {
			touch();
			return discover;
		}

		public Client getClient() {
			touch();
			return client;
		}

		@Override
		public String toString() {
			return "Conversation[" + discover.getHardwareAddress() + "/"
					+ discover.getTransactionId() + "]: age="
					+ (System.currentTimeMillis() - lastAccess) + ", client=" + client;
		}

		public void setApplicableServerAddress(
				InetSocketAddress applicableServerAddress) {
			this.applicableServerAddress = applicableServerAddress;
		}

		public InetSocketAddress getApplicableServerAddress() {
			return applicableServerAddress;
		}

		public void setClient(Client client) {
			this.client = client;
		}
	}

	/**
	 * 
	 */
	public static final int PXE_DHCP_PORT = 4011;
	private static final Logger logger = Logger
			.getLogger(PXEPrimerDhcpService.class);
	private Set<Realm> realms;
	private String defaultNextServerAddress;

	/**
	 * A map of on-going conversations.
	 */
	private static final Map<RequestID, Conversation> conversations = Collections
			.synchronizedMap(new HashMap<RequestID, Conversation>());

	public PXEPrimerDhcpService() throws DirectoryException {
		init();
	}

	/**
	 * @throws DirectoryException
	 */
	private void init() throws DirectoryException {
		final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
		lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
		lcd
				.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
		lcd.setCallbackHandler(new UsernamePasswordHandler("uid=admin,ou=system",
				"secret".toCharArray()));

		try {
			final ServerLocalSchemaProvider schemaProvider = new ServerLocalSchemaProvider();
			final Schema realmSchema = schemaProvider.getSchema(Realm.class, null);

			realms = LDAPDirectory.findAllRealms(lcd);

			for (final Realm realm : realms) {
				logger.info("Serving realm " + realm);
				realm.setSchema(realmSchema);
			}
		} catch (final DirectoryException e) {
			logger.fatal("Can't init directory", e);
			throw e;
		} catch (final SchemaLoadingException e) {
			throw new DirectoryException("Can't load schemas", e);
		}
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

	private static void expireConversations() {
		synchronized (conversations) {
			for (final Iterator<Conversation> i = conversations.values().iterator(); i
					.hasNext();) {
				final Conversation c = i.next();
				if (c.isExpired()) {
					if (logger.isInfoEnabled())
						logger.info("Expiring expired conversation " + c);
					i.remove();
				}
			}
		}
	}

	private boolean assertCorrectPort(InetSocketAddress localAddress, int port,
			DhcpMessage m) {
		// assert correct port
		if (localAddress.getPort() != port) {
			logger.warn("Ignoring " + m.getMessageType() + " on wrong port "
					+ localAddress.getPort());
			return false;
		}

		return true;
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

	/**
	 * Determine whether the given address is in the subnet specified by the
	 * network address and the address prefix.
	 * 
	 * @param ip
	 * @param network
	 * @param prefix
	 * @return
	 */
	private boolean isInSubnet(byte[] ip, byte[] network, short prefix) {
		if (ip.length != network.length)
			return false;

		if (prefix / 8 > ip.length)
			return false;

		int i = 0;
		while (prefix >= 8 && i < ip.length) {
			if (ip[i] != network[i])
				return false;
			i++;
			prefix -= 8;
		}
		final byte mask = (byte) ~((1 << 8 - prefix) - 1);

		return (ip[i] & mask) == (network[i] & mask);
	}

	/**
	 * @param discover
	 * @param inetAddress
	 * @param hwAddressString
	 * @throws DirectoryException
	 */
	private void trackUnrecognizedClient(DhcpMessage discover, DhcpMessage offer) {
		final String hwAddressString = discover.getHardwareAddress()
				.getNativeRepresentation();

		try {
			for (final Realm realm : realms)
				if ("true".equals(realm
						.getValue("BootOptions.TrackUnrecognizedPXEClients")))
					if (!(realm.getDirectory().list(UnrecognizedClient.class,
							new Filter("(&(macAddress={0})(!(l=*)))", hwAddressString),
							TypeMapping.SearchScope.SUBTREE).size() > 0)) {
						final VendorClassIdentifier vci = (VendorClassIdentifier) discover
								.getOptions().get(VendorClassIdentifier.class);

						final UnrecognizedClient uc = new UnrecognizedClient();

						final HostName hostnameOption = (HostName) offer.getOptions().get(
								HostName.class);
						uc.setName(hostnameOption != null
								? hostnameOption.getString()
								: offer.getAssignedClientAddress().toString());

						uc.setMacAddress(hwAddressString);
						uc.setIpHostNumber(offer.getAssignedClientAddress()
								.getHostAddress());
						uc.setDescription((vci != null ? vci.getString() : "")
								+ " first seen: " + new Date());

						realm.getDirectory().save(uc);
					}
		} catch (final DirectoryException e) {
			logger.error("Can't track unrecognized client", e);
		}
	}

	/**
	 * @param localAddress
	 * @param clientAddress
	 * @param request
	 * @return
	 */
	private String getLogDetail(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage request) {
		final VendorClassIdentifier vci = (VendorClassIdentifier) request
				.getOptions().get(VendorClassIdentifier.class);
		return " on "
				+ (null != localAddress ? localAddress : "<null>")
				+ " from "
				+ (null != clientAddress ? clientAddress : "<null>")
				+ " MAC="
				+ (null != request.getHardwareAddress()
						? request.getHardwareAddress()
						: "<null>") + " ID=" + (null != vci ? vci.getString() : "<???>");
	}

	/**
	 * Check if the request comes from a PXE client by looking at the
	 * VendorClassIdentifier.
	 * 
	 * @param request
	 * @return
	 */
	private boolean isPXEClient(DhcpMessage request) {
		final VendorClassIdentifier vci = (VendorClassIdentifier) request
				.getOptions().get(VendorClassIdentifier.class);
		return null != vci && vci.getString().startsWith("PXEClient:");
	}

	/**
	 * Check whether the PXE client which originated the message is elegible for
	 * PXE proxy service.
	 * 
	 * @param hwAddressString2
	 * 
	 * @param clientAddress
	 * @param request
	 * @return
	 */
	private Client getClient(String hwAddressString,
			InetSocketAddress clientAddress, DhcpMessage request) {
		try {
			Set<Client> found = null;

			for (final Realm realm : realms) {
				found = realm.getDirectory().list(Client.class,
						new Filter("(&(macAddress={0})(l=*))", hwAddressString),
						TypeMapping.SearchScope.SUBTREE);

				if (found.size() > 0) {
					if (found.size() > 1)
						logger.warn("Found more than one client for hardware address "
								+ request.getHardwareAddress());

					final Client c = found.iterator().next();
					c.initSchemas(realm);

					return c;
				}
			}

			return null;
		} catch (final DirectoryException e) {
			logger.error("Can't query for client for PXE service", e);
			return null;
		} catch (final SchemaLoadingException e) {
			logger.error("Can't query for client for PXE service", e);
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

	/**
	 * @param localAddress
	 * @param client
	 * @param reply
	 * @return
	 */
	private InetAddress getNextServerAddress(String paramName,
			InetSocketAddress localAddress, Client client) {
		InetAddress nsa = null;
		final String value = client.getValue(paramName);
		if (value != null && !value.contains("${myip}"))
			nsa = getInetAddress(value);

		if (null == nsa && null != defaultNextServerAddress)
			nsa = getInetAddress(defaultNextServerAddress);

		if (null == nsa)
			nsa = localAddress.getAddress();

		return nsa;
	}

	/**
	 * @param name
	 * @return
	 */
	private InetAddress getInetAddress(String name) {
		try {
			return InetAddress.getByName(name);
		} catch (final IOException e) {
			logger.warn("Invalid inet address: " + name);
			return null;
		}
	}
}
