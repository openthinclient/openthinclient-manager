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
package org.openthinclient.service.dhcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
import org.apache.directory.server.dhcp.options.vendor.RootPath;
import org.apache.directory.server.dhcp.service.AbstractDhcpService;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.common.model.schema.provider.ServerLocalSchemaProvider;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Filter;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author levigo
 */
public abstract class AbstractPXEService extends AbstractDhcpService implements Dhcp {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractPXEService.class);
	protected final String DEFAULT_CLIENT_MAC = "00:00:00:00:00:00";

	/**
	 * Key object used to index conversations.
	 */
	public static final class RequestID {
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
	public final class Conversation {
		private static final int CONVERSATION_EXPIRY = 60000;
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

	private Set<Realm> realms;
	private String defaultNextServerAddress;

	/**
	 * A map of on-going conversations.
	 */
	protected static final Map<RequestID, Conversation> conversations = Collections
			.synchronizedMap(new HashMap<RequestID, Conversation>());

	private LDAPConnectionDescriptor lcd;

	private Schema realmSchema;

	public AbstractPXEService() throws DirectoryException {
		init();
	}

	/**
	 * @throws DirectoryException
	 */
	private void init() throws DirectoryException {
		lcd = new LDAPConnectionDescriptor();
		lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
		lcd.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
		lcd.setCallbackHandler(new UsernamePasswordHandler("uid=admin,ou=system",
				System.getProperty("ContextSecurityCredentials", "secret").toCharArray()));

		try {
			final ServerLocalSchemaProvider schemaProvider = new ServerLocalSchemaProvider();
			realmSchema = schemaProvider.getSchema(Realm.class, null);
			realms = LDAPDirectory.findAllRealms(lcd);

			for (final Realm realm : realms) {
				logger.info("Serving realm " + realm);
				realm.setSchema(realmSchema);
			}
		} catch (final DirectoryException e) {
			logger.error("Can't init directory", e);
			throw e;
		} catch (final SchemaLoadingException e) {
			throw new DirectoryException("Can't load schemas", e);
		}
	}

	public boolean reloadRealms() throws DirectoryException {
		try {
			realms = LDAPDirectory.findAllRealms(lcd); 

			for (final Realm realm : realms) {
				logger.info("Serving realm " + realm);
				realm.setSchema(realmSchema);
			}
			return true;
		} catch (final DirectoryException e) {
			logger.error("Can't init directory", e);
			throw e;
		}
	}

	protected static void expireConversations() {
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

	protected boolean assertCorrectPort(InetSocketAddress localAddress, int port,
			DhcpMessage m) {
		// assert correct port
		if (localAddress.getPort() != port) {
			logger.debug("Ignoring " + m.getMessageType() + " on wrong port "
					+ localAddress.getPort());
			return false;
		}

		return true;
	}

	/**
	 * Determine whether the given address is the all-zero address 0.0.0.0
	 * 
	 * @param a
	 * @return
	 */
	protected static boolean isZeroAddress(InetAddress a) {
		final byte addr[] = a.getAddress();
		for (int i = 0; i < addr.length; i++)
			if (addr[i] != 0)
				return false;

		return true;
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
	protected static boolean isInSubnet(byte[] ip, byte[] network, short prefix) {
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
	 * Track an unrecognized client.
	 * 
	 * @param discover the initial discover message sent by the client
	 * @param hostname the client's host name (if known)
	 * @param clientAddress the client's ip address (if known)
	 */
	protected void trackUnrecognizedClient(DhcpMessage discover, String hostname,
			String clientAddress) {
		final String hwAddressString = discover.getHardwareAddress()
				.getNativeRepresentation();

		try {
			for (final Realm realm : realms)
				if ("true".equals(realm
						.getValue("BootOptions.TrackUnrecognizedPXEClients")))
					if (!(realm
							.getDirectory()
							.list(UnrecognizedClient.class,
									new Filter("(&(macAddress={0})(!(l=*)))", hwAddressString),
									TypeMapping.SearchScope.SUBTREE).size() > 0)) {
						final VendorClassIdentifier vci = (VendorClassIdentifier) discover
								.getOptions().get(VendorClassIdentifier.class);

						final UnrecognizedClient uc = new UnrecognizedClient();

						// invent a client name, if it is not yet known.
						if (null == hostname)
							hostname = hwAddressString;

						uc.setName(hostname);

						uc.setMacAddress(hwAddressString);
						uc.setIpHostNumber(clientAddress);
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
	protected String getLogDetail(InetSocketAddress localAddress,
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
	protected boolean isPXEClient(DhcpMessage request) {
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
	protected Client getClient(String hwAddressString,
			InetSocketAddress clientAddress, DhcpMessage request) {
		try {
			Set<Client> found = null;
			Client client = null;

			for (final Realm realm : realms) {
				found = realm.getDirectory().list(Client.class,
						new Filter("(&(macAddress={0})(l=*))", hwAddressString),
						TypeMapping.SearchScope.SUBTREE);

				if (found.size() > 0) {
					if (found.size() > 1)
						logger.warn("Found more than one client for hardware address "
								+ request.getHardwareAddress());

					client = found.iterator().next();
					client.initSchemas(realm);

					return client;
				} else if (found.size() == 0) {
					final String pxeServicePolicy = realm
							.getValue("BootOptions.PXEServicePolicy");
					if ("AnyClient".equals(pxeServicePolicy)) {
						found = realm.getDirectory().list(Client.class,
								new Filter("(&(macAddress={0})(l=*))", DEFAULT_CLIENT_MAC),
								TypeMapping.SearchScope.SUBTREE);
						if (found.size() > 0) {
							if (found.size() > 1)
								logger
										.warn("Found more than one client for default hardware address "
												+ DEFAULT_CLIENT_MAC);

							client = found.iterator().next();
							client.initSchemas(realm);

							return client;
						}
					}
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

	/**
	 * @param localAddress
	 * @param client
	 * @param reply
	 * @return
	 */
	protected InetAddress getNextServerAddress(String paramName,
			InetSocketAddress localAddress, Client client) {
		InetAddress nsa = null;
		final String value = client.getValue(paramName);
		if (value != null && !value.contains("${myip}"))
			nsa = safeGetInetAddress(value);

		if (null == nsa && null != defaultNextServerAddress)
			nsa = safeGetInetAddress(defaultNextServerAddress);

		if (null == nsa)
			nsa = localAddress.getAddress();

		return nsa;
	}

	/**
	 * @param name
	 * @return
	 */
	private InetAddress safeGetInetAddress(String name) {
		try {
			return InetAddress.getByName(name);
		} catch (final IOException e) {
			logger.warn("Invalid inet address: " + name);
			return null;
		}
	}

	/*
	 * @see
	 * org.apache.directory.server.dhcp.service.AbstractDhcpService#handleREQUEST
	 * (java.net.InetSocketAddress,
	 * org.apache.directory.server.dhcp.messages.DhcpMessage)
	 */
	@Override
	protected DhcpMessage handleREQUEST(InetSocketAddress localAddress,
			InetSocketAddress clientAddress, DhcpMessage request)
			throws DhcpException {
		// detect PXE client
		if (!isPXEClient(request)) {
			if (logger.isDebugEnabled())
				logger.debug("Ignoring non-PXE REQUEST"
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
				if (logger.isDebugEnabled())
					logger.debug("Ignoring PXE REQUEST for server " + serverIdentOption);
				return null; // not me!
			}

			final DhcpMessage reply = initGeneralReply(
					conversation.getApplicableServerAddress(), request);

			reply.setMessageType(MessageType.DHCPACK);

			final OptionsField options = reply.getOptions();

			reply.setNextServerAddress(getNextServerAddress(
					"BootOptions.TFTPBootserver",
					conversation.getApplicableServerAddress(), client));

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
	 * Bind service to the appropriate sockets for this type of service.
	 * 
	 * @param acceptor the {@link SocketAcceptor} to be bound
	 * @param handler the {@link IoHandler} to use
	 * @param config the {@link IoServiceConfig} to use
	 * 
	 * @return
	 * @throws IOException
	 */
	public abstract void init(IoAcceptor acceptor, IoHandler handler,
			IoServiceConfig config) throws IOException;
}
