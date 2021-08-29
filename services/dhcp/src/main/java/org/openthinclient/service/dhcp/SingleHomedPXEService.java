package org.openthinclient.service.dhcp;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.openthinclient.ldap.DirectoryException;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * This PXE service implementation assumes a single-homed server host. The PXE
 * proxy service is bound to the default address. Replies are sent from a fixed
 * server address which can be configured statically. If no static address is
 * configured, the first non-loopback local interface is used.
 * 
 * @author levigo
 */
public class SingleHomedPXEService extends BasePXEService {
	private InetAddress serverAddress;

  public SingleHomedPXEService(RealmService realmService, ClientService clientService, UnrecognizedClientService unrecognizedClientService) throws DirectoryException {
    super(realmService, clientService, unrecognizedClientService);
  }

	@Override
	public void init(IoAcceptor acceptor, IoHandler handler, IoServiceConfig config) throws IOException {
		logger.warn("-------------------------------------------------------------");
		logger.warn("  Using SingleHomedPXEService implementation. ");
		logger.warn("  This type of service might be problematic on multi-homed systems.");
		logger.warn("  (for more details, see log messages with level INFO)");
		logger.info("");

		InetAddress localAddress = getConfiguredLocalAddress();

		if (null == localAddress)
			// auto-determine local address
			outer : for (final Enumeration i = NetworkInterface
					.getNetworkInterfaces(); i.hasMoreElements();) {
				final NetworkInterface nif = (NetworkInterface) i.nextElement();
				if (!nif.isLoopback() && !nif.isPointToPoint() && !nif.isVirtual()
						&& nif.isUp())
					for (final Enumeration j = nif.getInetAddresses(); j
							.hasMoreElements();) {
						final InetAddress address = (InetAddress) j.nextElement();
						if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
							localAddress = address;
							break outer;
						}
					}
			}

		if (null == localAddress)
			logger.warn("  Could not determine ANY local address to use. PXE service will be unavailable");
		else {
			serverAddress = localAddress;
			acceptor.bind(new InetSocketAddress(serverAddress, 67), handler, config);
			logger.info("  Binding on " + serverAddress);

			final InetSocketAddress pxePort = new InetSocketAddress(localAddress, 4011);
			acceptor.bind(pxePort, handler, config);
			logger.info("  Binding on " + pxePort);
		}

		logger.warn("-------------------------------------------------------------");
	}

	/**
	 * Determine a configured static local address.
	 * 
	 * @return
	 */
	private InetAddress getConfiguredLocalAddress() {
		return serverAddress;
	}
}
