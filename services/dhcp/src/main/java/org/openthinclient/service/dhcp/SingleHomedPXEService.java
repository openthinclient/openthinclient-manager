package org.openthinclient.service.dhcp;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.openthinclient.ldap.DirectoryException;

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

	public SingleHomedPXEService() throws DirectoryException {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openthinclient.dhcp.BasePXEService#determineServerAddress(java.net.InetSocketAddress)
	 */
	@Override
	protected InetSocketAddress determineServerAddress(
			InetSocketAddress localAddress) {
		// since we bound individually, we can use the local address.
		return localAddress;
	}

	@Override
	public void init(IoAcceptor acceptor, IoHandler handler, IoServiceConfig config) throws IOException {
		logger
				.warn("-------------------------------------------------------------");
		logger.warn("  Using SingleHomedPXEService implementation. ");
		logger
				.warn("  This type of service might be problematic on multi-homed systems.");
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
			// TODO: JN this port has been changed manually to run without sudo - this must be configured
			acceptor.bind(new InetSocketAddress(serverAddress, 10067), handler, config);
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
