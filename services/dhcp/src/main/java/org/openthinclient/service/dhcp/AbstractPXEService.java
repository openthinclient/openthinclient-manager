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

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.ArchType;
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
import org.openthinclient.service.store.ClientBootData;
import org.openthinclient.service.store.LDAPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;

/**
 * @author levigo
 */
public abstract class AbstractPXEService extends AbstractDhcpService {

  public static final int PXE_DHCP_PORT = 4011;
  /**
   * A map of on-going conversations.
   */
  protected static final Map<RequestID, Conversation> conversations = Collections
          .synchronizedMap(new HashMap<RequestID, Conversation>());
  private static final Logger logger = LoggerFactory.getLogger(AbstractPXEService.class);
  private String defaultNextServerAddress;
  private volatile boolean trackUnrecognizedPXEClients;
  private DhcpServiceConfiguration.PXEPolicy policy;

  protected static void expireConversations() {
    synchronized (conversations) {
      for (final Iterator<Conversation> i = conversations.values().iterator(); i
              .hasNext(); ) {
        final Conversation c = i.next();
        if (c.isExpired()) {
          if (logger.isInfoEnabled())
            logger.info("Expiring expired conversation " + c);
          i.remove();
        }
      }
    }
  }

  /**
   * Determine whether the given address is the all-zero address 0.0.0.0
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

  public boolean isTrackUnrecognizedPXEClients() {
    return trackUnrecognizedPXEClients;
  }

  public void setTrackUnrecognizedPXEClients(boolean trackUnrecognizedPXEClients) {
    this.trackUnrecognizedPXEClients = trackUnrecognizedPXEClients;
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

  private String getBootfileName(DhcpMessage message, ClientBootData bootData) {
    // new simplified schema
    boolean safe = "safe".equals(bootData.get("BootOptions.BootMode", null));
    switch(ArchType.fromMessage(message)) {
    case UEFI32:
      return safe ? "ipxe32.efi" : "syslinux32.efi";
    case UEFI64:
      return safe ? "ipxe64.efi" : "syslinux64.efi";
    default:
      // old schema value if installation is not yet migrated
      String bootfile = bootData.get("BootOptions.BootfileName", null);
      if (bootfile != null) {
        return bootfile;
      }
      return safe ? "/pxelinux.0" : "/lpxelinux.0";
    }
  }

  protected String getBootFileURI(Conversation conversation) {
    String bootFileName = null;
    switch(conversation.getArchType()) {
      case HTTP32:
        bootFileName = "ipxe32.efi";
        break;
      case HTTP64:
        bootFileName = "ipxe64.efi";
        break;
      default:
        logger.error("Could not determine boot file for {}",
                      conversation.getArchType());
        bootFileName = "ipxe64.efi";
    }

    InetSocketAddress serverAddress = conversation.getApplicableServerAddress();
    return String.format("http://%s:8080/openthinclient/files/tftp/%s",
                          serverAddress.getAddress().getHostAddress(),
                          bootFileName);
  }

  /**
   * Track an unrecognized client.
   *
   * @param discover      the initial discover message sent by the client
   * @param hostname      the client's host name (if known)
   * @param clientAddress the client's ip address (if known)
   */
  protected void trackUnrecognizedClient(DhcpMessage discover, String ipHostNumber) {
    if (!isTrackUnrecognizedPXEClients())
      return;

    final String hwAddressString = discover.getHardwareAddress()
            .getNativeRepresentation().toLowerCase();

    try (LDAPConnection ldapCon = new LDAPConnection()) {
      ldapCon.updateUnrecognizedClient(
          hwAddressString,
          ipHostNumber != null? ipHostNumber : "0.0.0.0",
          String.format("last seen: %s", Instant.now()));
    } catch (NamingException /* | InterruptedException */ ex) {
      logger.error("Can't track unrecognized client", ex);
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
   * @param localAddress
   * @param bootData
   * @return
   */
  protected InetAddress getNextServerAddress(String property,
                                             InetSocketAddress localAddress,
                                             ClientBootData bootData) {
    InetAddress nsa = null;
    final String value = bootData.get(property, null);
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

    /**
     * Determine the server address to use.
     *
     * @param localAddress the address of the socket which received the request.
     * @param message the DHCP message this server is responding to
     * @return
     */
    protected InetSocketAddress determineServerAddress(
            InetSocketAddress localAddress, DhcpMessage message) {
        // Individually bound services, can use the local address.
        // Others may need to override.
        return localAddress;
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

    ArchType archType = ArchType.fromMessage(request);

    if (archType.isHTTP()) {
      return null;
    }

    // detect PXE client
    if (!archType.isPXEClient()) {
      if (logger.isDebugEnabled())
        logger.debug("Ignoring non-PXE REQUEST"
                + getLogDetail(localAddress, clientAddress, request));
      return null;
    }

    if (logger.isInfoEnabled())
      logger.info("Got PXE REQUEST"
              + getLogDetail(localAddress, clientAddress, request));

    // clientAddress must be set
    if (isZeroAddress(clientAddress.getAddress())) {
      if (logger.isDebugEnabled())
        logger.debug("Ignoring PXE REQUEST from 0.0.0.0"
            + getLogDetail(localAddress, clientAddress, request));
      return null;
    }

    // we don't react to requests here, unless they go to port 4011
    if (!assertCorrectPort(localAddress, 4011, request))
      return null;

    // find conversation
    final RequestID id = new RequestID(request);
    Conversation conversation = conversations.get(id);

    if (null == conversation) {
      if (archType.isUEFI()) {
        // Some UEFI PXE implementations don't set the correct transaction id
        // in their last request. In order to be able to serve those devices
        // we simply begin a new "conversation" and send the last ACK with the
        // PXE boot data.
        String hwAddressString = request.getHardwareAddress().getNativeRepresentation();
        ClientBootData bootData = ClientBootData.load(hwAddressString);
        if(bootData == null) {
          // client not eligible for PXE proxy service
          return null;
        }
        logger.info("Got UEFI PXE REQUEST for which there is no conversation. Serving anyway."
            + getLogDetail(localAddress, clientAddress, request));
        conversation = new Conversation(request, archType);
        synchronized (conversation) {
            conversation.setClient(bootData);
            InetSocketAddress serverAddress = determineServerAddress(localAddress, request);
            conversation.setApplicableServerAddress(serverAddress);
        }
      } else {
        logger.info("Got BIOS PXE REQUEST for which there is no conversation"
            + getLogDetail(localAddress, clientAddress, request));
        return null;
      }
    }

    synchronized (conversation) {
      if (conversation.isExpired()) {
        if (logger.isInfoEnabled())
          logger.info("Got PXE REQUEST for an expired conversation: "
                  + conversation);
        conversations.remove(id);
        return null;
      }

      final ClientBootData bootData = conversation.getBootData();
      if (null == bootData) {
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

      try {
        bootData.saveIP(clientAddress.getAddress().toString().split("/")[1]);
      } catch (Exception ex) {
        logger.error("Could not save client IP", ex);
      }

      final InetSocketAddress serverAddress = conversation.getApplicableServerAddress();
      final DhcpMessage reply = initGeneralReply(serverAddress, request);

      reply.setMessageType(MessageType.DHCPACK);

      final OptionsField options = reply.getOptions();

      final VendorClassIdentifier vci = new VendorClassIdentifier();
      vci.setString("PXEClient");
      options.add(vci);

      reply.setNextServerAddress(getNextServerAddress(
              "BootOptions.TFTPBootserver", serverAddress, bootData));

      final String rootPath = getNextServerAddress(
          "BootOptions.NFSRootserver",
          serverAddress,
          bootData).getHostAddress() + ":" + bootData.get("BootOptions.NFSRootPath", null);
      options.add(new RootPath(rootPath));

      reply.setBootFileName(getBootfileName(request, bootData));

      if (logger.isInfoEnabled())
        logger.info("Sending PXE proxy ACK rootPath=" + rootPath
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
   * @param handler  the {@link IoHandler} to use
   * @param config   the {@link IoServiceConfig} to use
   */
  public abstract void init(IoAcceptor acceptor, IoHandler handler,
                            IoServiceConfig config) throws IOException;

  public DhcpServiceConfiguration.PXEPolicy getPolicy() {
    return policy;
  }

  public void setPolicy(DhcpServiceConfiguration.PXEPolicy policy) {
    this.policy = policy;
  }

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
    private final ArchType archType;
    private ClientBootData bootData;
    private DhcpMessage offer;
    private long lastAccess;
    private InetSocketAddress applicableServerAddress;

    public Conversation(DhcpMessage discover, ArchType archType) {
      this.discover = discover;
      this.archType = archType;
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

    public ArchType getArchType() {
      return archType;
    }

    public ClientBootData getBootData() {
      touch();
      return bootData;
    }

    public void setClient(ClientBootData bootData) {
      this.bootData = bootData;
    }

    @Override
    public String toString() {
      return "Conversation[" + discover.getHardwareAddress() + "/"
              + discover.getTransactionId() + "]: age="
              + (System.currentTimeMillis() - lastAccess) + ", client=" + bootData;
    }

    public InetSocketAddress getApplicableServerAddress() {
      return applicableServerAddress;
    }

    public void setApplicableServerAddress(
            InetSocketAddress applicableServerAddress) {
      this.applicableServerAddress = applicableServerAddress;
    }
  }
}
