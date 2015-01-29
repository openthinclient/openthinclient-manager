package org.openthinclient.dhcp;

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
 * This PXE service implementation works by binding to all addresses on all
 * interfaces individually. This works, if broadcast packets are also received
 * by sockets not bound to the default address, but individual addresses. This
 * works fine on MS Windows and Linux running within XEN, but fails for most (?)
 * other UNIX servers.
 * 
 * For details see
 * {@linkplain https://issues.openthinclient.org/otc/browse/SUITE-39}
 * 
 * @author levigo
 */
public class BindToAddressPXEService extends BasePXEService {
	public BindToAddressPXEService() throws DirectoryException {
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
	public void init(IoAcceptor acceptor, IoHandler handler,
			IoServiceConfig config) throws IOException {
		logger
				.warn("-------------------------------------------------------------");
		logger.warn("  Using BindToAddressPXEService implementation. ");
		logger.warn("  This type of service will not work on most UNIX systems.");
		logger.warn("  (for more details, see log messages with level INFO)");
		logger.info("");

		// To properly serve DHCP, we must bind to all local addresses
		// individually, in order to be able to distinguish, from which network
		// (on a multi-homed machine) a broadcast came.
		// bind to all local ports. Gobble up all addresses we can find.

		// This is a Problem. See:
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4212324

		for (final Enumeration i = NetworkInterface.getNetworkInterfaces(); i
				.hasMoreElements();) {
			final NetworkInterface nif = (NetworkInterface) i.nextElement();
			for (final Enumeration j = nif.getInetAddresses(); j.hasMoreElements();) {
				final InetAddress address = (InetAddress) j.nextElement();

				if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
					// we bind to both the standard DHCP port AND the PXE port
					final InetSocketAddress dhcpPort = new InetSocketAddress(address, 67);
					acceptor.bind(dhcpPort, handler, config);
					logger.info("Listening on " + dhcpPort);

					final InetSocketAddress pxePort = new InetSocketAddress(address, 4011);
					acceptor.bind(pxePort, handler, config);
					logger.info("Listening on " + pxePort);
				}
			}
		}
		logger
				.warn("-------------------------------------------------------------");
	}
}
