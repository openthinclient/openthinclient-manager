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
 *******************************************************************************/
package org.openthinclient.dhcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Set;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.MessageType;
import org.apache.directory.server.dhcp.options.AddressOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.ServerIdentifier;
import org.apache.directory.server.dhcp.options.dhcp.VendorClassIdentifier;
import org.apache.directory.server.dhcp.options.vendor.RootPath;
import org.apache.directory.server.dhcp.service.AbstractDhcpService;
import org.apache.log4j.Logger;
import org.openthinclient.common.directory.LDAPConnectionDescriptor;
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
import org.openthinclient.ldap.TypeMapping;


/**
 * @author levigo
 */
public class PXEPrimerDhcpService extends AbstractDhcpService {
	/**
	 * 
	 */
	public static final int PXE_DHCP_PORT = 4011;
	private static final Logger logger = Logger
			.getLogger(PXEPrimerDhcpService.class);
	private Set<Realm> realms;
	private String defaultNextServerAddress;

	public PXEPrimerDhcpService() throws DirectoryException {
		init();
	}

	/**
	 * @throws DirectoryException
	 */
	private void init() throws DirectoryException {
		LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
		lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
		lcd
				.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
		lcd.setCallbackHandler(new UsernamePasswordHandler("uid=admin,ou=system",
				"secret".toCharArray()));

		try {
			ServerLocalSchemaProvider schemaProvider = new ServerLocalSchemaProvider();
			Schema realmSchema = schemaProvider.getSchema(Realm.class, null);

			realms = LDAPDirectory.findAllRealms(lcd);

			for (Realm realm : realms) {
				logger.info("Serving realm " + realm);
				realm.setSchema(realmSchema);
			}
		} catch (DirectoryException e) {
			logger.fatal("Can't init directory", e);
			throw e;
		} catch (SchemaLoadingException e) {
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
		// detect PXE client
		if (!isPXEClient(request)) {
			if (logger.isInfoEnabled())
				logger.info("Ignoring non-PXE DISCOVER"
						+ getLogDetail(localAddress, clientAddress, request));
			return null;
		}

		if (logger.isInfoEnabled())
			logger.info("Got PXE DISCOVER"
					+ getLogDetail(localAddress, clientAddress, request));

		// check whether client is eligible for PXE service
		String hwAddressString = request.getHardwareAddress()
				.getNativeRepresentation();
		if (getClient(hwAddressString, clientAddress, request) == null) {
			logger.info("Client not eligible for PXE proxy service");

			// track unrecognized clients?
			trackUnrecognizedClient(request, hwAddressString);
			return null;
		}

		logger.info("Sending PXE proxy offer");

		// send 0.0.0.0 offer.
		DhcpMessage reply = initGeneralReply(localAddress, request);

		reply.setMessageType(MessageType.DHCPOFFER);

		return reply;
	}

	/**
	 * @param request
	 * @param hwAddressString
	 * @throws DirectoryException
	 */
	private void trackUnrecognizedClient(DhcpMessage request,
			String hwAddressString) {
		try {
			for (Realm realm : realms) {
				if ("true".equals(realm
						.getValue("BootOptions.TrackUnrecognizedPXEClients"))) {
					if (!(realm.getDirectory().list(UnrecognizedClient.class, "",
							new Filter("(&(macAddress={0})(!(l=*)))", hwAddressString),
							TypeMapping.SearchScope.SUBTREE).size() > 0)) {
						VendorClassIdentifier vci = ((VendorClassIdentifier) request
								.getOptions().get(VendorClassIdentifier.class));

						UnrecognizedClient uc = new UnrecognizedClient();
						uc.setName(hwAddressString);
						uc.setMacAddress(hwAddressString);
						uc.setDescription((vci != null ? vci.getString() : "")
								+ " first seen: " + new Date());

						realm.getDirectory().save(uc);
					}
				}
			}
		} catch (DirectoryException e) {
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
		VendorClassIdentifier vci = ((VendorClassIdentifier) request.getOptions()
				.get(VendorClassIdentifier.class));
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
		VendorClassIdentifier vci = (VendorClassIdentifier) request.getOptions()
				.get(VendorClassIdentifier.class);
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

			for (Realm realm : realms) {
				found = realm.getDirectory().list(Client.class, null,
						new Filter("(&(macAddress={0})(l=*))", hwAddressString),
						TypeMapping.SearchScope.SUBTREE);

				if (found.size() > 0) {
					if (found.size() > 1)
						logger.warn("Found more than one client for hardware address "
								+ request.getHardwareAddress());

					Client c = found.iterator().next();
					c.initSchemas(realm);

					return c;
				}
			}

			return null;
		} catch (DirectoryException e) {
			logger.error("Can't query for client for PXE service", e);
			return null;
		} catch (SchemaLoadingException e) {
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
		if (localAddress.getPort() != PXE_DHCP_PORT) {
			if (logger.isInfoEnabled())
				logger.info("Ignoring PXE REQUEST on port!=" + PXE_DHCP_PORT);
			return null;
		}

		// check whether client is eligible for PXE service
		Client client = getClient(request.getHardwareAddress()
				.getNativeRepresentation(), clientAddress, request);
		if (null == client) {
			if (logger.isInfoEnabled())
				logger.info("Client not eligible for PXE proxy service");
			return null;
		}

		// check server ident
		AddressOption serverIdentOption = (AddressOption) request.getOptions().get(
				ServerIdentifier.class);
		if (null != serverIdentOption
				&& serverIdentOption.getAddress().isAnyLocalAddress()) {
			if (logger.isInfoEnabled())
				logger.info("Ignoring PXE REQUEST for server " + serverIdentOption);
			return null; // not me!
		}

		DhcpMessage reply = initGeneralReply(localAddress, request);

		reply.setMessageType(MessageType.DHCPACK);

		OptionsField options = reply.getOptions();

		reply.setNextServerAddress(getNextServerAddress(
				"BootOptions.TFTPBootserver", localAddress, client));

		String rootPath = getNextServerAddress("BootOptions.NFSRootserver",
				localAddress, client).getHostAddress()
				+ ":" + client.getValue("BootOptions.NFSRootPath");
		options.add(new RootPath(rootPath));

		reply.setBootFileName(client.getValue("BootOptions.BootfileName"));

		if (logger.isInfoEnabled())
			logger.info("Sending PXE proxy ACK rootPath=" + rootPath
					+ " bootFileName=" + reply.getBootFileName() + " nextServerAddress="
					+ reply.getNextServerAddress().getHostAddress() + " reply=" + reply);
		return reply;
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
		String value = client.getValue(paramName);
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
		} catch (IOException e) {
			logger.warn("Invalid inet address: " + name);
			return null;
		}
	}
}
