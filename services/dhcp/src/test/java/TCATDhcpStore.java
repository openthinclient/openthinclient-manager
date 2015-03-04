
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.service.Lease;
import org.apache.directory.server.dhcp.store.AbstractDhcpStore;
import org.apache.directory.server.dhcp.store.DhcpConfigElement;
import org.apache.directory.server.dhcp.store.Host;
import org.apache.directory.server.dhcp.store.Subnet;

/**
 * Very simple proof-of-concept implementation of a DhcpStore.
 */
public class TCATDhcpStore extends AbstractDhcpStore {
	// private static final String DEFAULT_INITIAL_CONTEXT_FACTORY =
	// "org.apache.directory.server.core.jndi.CoreContextFactory";

	// a map of current leases
	private final Map leases = new HashMap();

	private final List subnets = new ArrayList();

	public TCATDhcpStore() {
		try {
			subnets.add(new Subnet(InetAddress.getByName("192.168.168.0"),
					InetAddress.getByName("255.255.255.0"), InetAddress
							.getByName("192.168.168.159"), InetAddress
							.getByName("192.168.168.179")));
		} catch (final UnknownHostException e) {
			throw new RuntimeException("Can't init", e);
		}
	}

	protected DirContext getContext() throws NamingException {
		final Hashtable env = new Hashtable();
		env
				.put(Context.INITIAL_CONTEXT_FACTORY,
						"com.sun.jndi.ldap.LdapCtxFactory");
		// env.put( Context.INITIAL_CONTEXT_FACTORY,
		// DEFAULT_INITIAL_CONTEXT_FACTORY );
		env.put(Context.PROVIDER_URL, "ldap://localhost:10389/dc=tcat,dc=test");

		return new InitialDirContext(env);
	}

	/**
	 * @param hardwareAddress
	 * @param existingLease
	 * @return
	 */
	@Override
	protected Lease findExistingLease(HardwareAddress hardwareAddress,
			Lease existingLease) {
		if (leases.containsKey(hardwareAddress))
			existingLease = (Lease) leases.get(hardwareAddress);
		return existingLease;
	}

	/**
	 * @param hardwareAddress
	 * @return
	 * @throws DhcpException
	 */
	@Override
	protected Host findDesignatedHost(HardwareAddress hardwareAddress)
			throws DhcpException {
		try {
			final DirContext ctx = getContext();
			try {
				final String filter = "(&(objectclass=ipHost)(objectclass=ieee802Device)(macaddress={0}))";
				final SearchControls sc = new SearchControls();
				sc.setCountLimit(1);
				sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
				final NamingEnumeration ne = ctx.search("", filter,
						new Object[]{hardwareAddress.toString()}, sc);

				if (ne.hasMoreElements()) {
					final SearchResult sr = (SearchResult) ne.next();
					final Attributes att = sr.getAttributes();
					final Attribute ipHostNumberAttribute = att.get("iphostnumber");
					if (ipHostNumberAttribute != null) {
						final InetAddress clientAddress = InetAddress
								.getByName((String) ipHostNumberAttribute.get());
						final Attribute cnAttribute = att.get("cn");
						return new Host(cnAttribute != null
								? (String) cnAttribute.get()
								: "unknown", clientAddress, hardwareAddress);
					}
				}
			} catch (final Exception e) {
				throw new DhcpException("Can't lookup lease", e);
			} finally {
				ctx.close();
			}
		} catch (final NamingException e) {
			throw new DhcpException("Can't lookup lease", e);
		}

		return null;
	}

	/**
	 * Find the subnet for the given client address.
	 * 
	 * @param clientAddress
	 * @return
	 */
	@Override
	protected Subnet findSubnet(InetAddress clientAddress) {
		for (final Iterator i = subnets.iterator(); i.hasNext();) {
			final Subnet subnet = (Subnet) i.next();
			if (subnet.contains(clientAddress))
				return subnet;
		}
		return null;
	}

	/*
	 * @see org.apache.directory.server.dhcp.store.AbstractDhcpStore#updateLease(org.apache.directory.server.dhcp.service.Lease)
	 */
	@Override
	public void updateLease(Lease lease) {
		leases.put(lease.getHardwareAddress(), lease);
	}

	/*
	 * @see org.apache.directory.server.dhcp.store.AbstractDhcpStore#getOptions(org.apache.directory.server.dhcp.store.DhcpConfigElement)
	 */
	@Override
	protected OptionsField getOptions(DhcpConfigElement element) {
		// we don't have groups, classes, etc. yet.
		return element.getOptions();
	}

	/*
	 * @see org.apache.directory.server.dhcp.store.AbstractDhcpStore#getProperties(org.apache.directory.server.dhcp.store.DhcpConfigElement)
	 */
	@Override
	protected Map getProperties(DhcpConfigElement element) {
		// we don't have groups, classes, etc. yet.
		return element.getProperties();
	}
}
