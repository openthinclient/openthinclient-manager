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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.dhcp.VendorClassIdentifier;
import org.apache.directory.server.dhcp.options.vendor.HostName;
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
public abstract class AbstractPXEService extends AbstractDhcpService {
	private static final Logger logger = Logger
			.getLogger(AbstractPXEService.class);

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

	private Set<Realm> realms;
	private String defaultNextServerAddress;

	/**
	 * A map of on-going conversations.
	 */
	protected static final Map<RequestID, Conversation> conversations = Collections
			.synchronizedMap(new HashMap<RequestID, Conversation>());

	public AbstractPXEService() throws DirectoryException {
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
			logger.warn("Ignoring " + m.getMessageType() + " on wrong port "
					+ localAddress.getPort());
			return false;
		}

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
	 * @param discover
	 * @param inetAddress
	 * @param hwAddressString
	 * @throws DirectoryException
	 */
	protected void trackUnrecognizedClient(DhcpMessage discover, DhcpMessage offer) {
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
