package org.openthinclient.service.dhcp;

import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * This PXE service implementation assumes a single-homed server host. The PXE
 * proxy service is bound to the default address. Replies are sent from a fixed
 * server address which can be configured statically. If no static address is
 * configured, the first non-loopback local interface is used.
 *
 * @author levigo
 */
public class SingleHomedBroadcastPXEService extends BasePXEService {
	private InetAddress serverAddress;

	@Override
	protected InetSocketAddress determineServerAddress(
			InetSocketAddress localAddress, DhcpMessage message) {
		return new InetSocketAddress(serverAddress, 67);
	}

	@Override
	public void init(IoAcceptor acceptor, IoHandler handler,
			IoServiceConfig config) throws IOException {
		logger.warn("-------------------------------------------------------------");
		logger.warn("  Using SingleHomedBroadcastPXEService implementation. ");
		logger.warn("  This type of service might be problematic on multi-homed systems.");
		logger.warn("  (for more details, see log messages with level INFO)");
		logger.info("");

		serverAddress = getConfiguredServerAddress();

		final InetSocketAddress listenPort = new InetSocketAddress(67);
		acceptor.bind(listenPort, handler, config);
		logger.info("  Binding on " + listenPort);

		final InetSocketAddress pxePort = new InetSocketAddress(4011);
		acceptor.bind(pxePort, handler, config);
		logger.info("  Binding on " + pxePort);

		logger.warn("-------------------------------------------------------------");
	}

	/**
	 * Determine a configured static server address.
	 *
	 * @return
	 */
	private InetAddress getConfiguredServerAddress() {
		try {
			// auto-determine local address
			InetAddress ifAddress = null;
			outer : for (final Enumeration i = NetworkInterface
					.getNetworkInterfaces(); i.hasMoreElements();) {
				final NetworkInterface nif = (NetworkInterface) i.nextElement();
				if (!nif.isLoopback() && !nif.isPointToPoint() && !nif.isVirtual()
						&& nif.isUp())
					for (final Enumeration j = nif.getInetAddresses(); j
							.hasMoreElements();) {
						final InetAddress address = (InetAddress) j.nextElement();
						if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
							ifAddress = address;
							logger.info("  Using address " + ifAddress + " as source IP");
							break outer;
						}
					}
			}
			if (null == ifAddress) {
				logger.error("  No non loopback InterfaceAddress found at all");
				return null;
			}
			return ifAddress;
		} catch (final SocketException e) {
			logger.error("  Can't determine network interface");
		}
		return null;
	}
}
