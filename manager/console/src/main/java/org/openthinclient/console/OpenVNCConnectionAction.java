package org.openthinclient.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;

public class OpenVNCConnectionAction extends NodeAction {

	private static final long serialVersionUID = 1L;

	private String case1 = "0.0.0.0";
	private String case2 = "127.0.0.1";
	private static final Pattern IPADDRESS_PATTERN = Pattern
			.compile("([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
					+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
					+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
					+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])");
	private static String fileName = "/openthinclient/files/var/log/syslog.log";

	/*
	 * @see org.openide.util.actions.CallableSystemAction#asynchronous()
	 */
	@Override
	protected boolean asynchronous() {
		return true;
	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	@Override
	protected boolean enable(Node[] arg0) {

		return true;
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		return Messages.getString("action." + this.getClass().getSimpleName()); //$NON-NLS-1$
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getHelpCtx()
	 */
	@Override
	public HelpCtx getHelpCtx() {
		return null;
	}

	@SuppressWarnings({ "deprecation", "unused" })
	@Override
	protected void performAction(Node[] nodes) {

		for (final Node node : nodes) {
			final String ipAddress = ((Client) (DirectoryObject) node
					.getLookup().lookup(DirectoryObject.class))
					.getIpHostNumber();
			if (ipAddress.equals(case1) || ipAddress.equals(case2)) {

				String macAddress = ((Client) (DirectoryObject) node
						.getLookup().lookup(DirectoryObject.class))
						.getMacAddress();
				String logIpAddress = null;
				String homeServer = "";
				Realm realm = null;
				if (null == node) {
					if (null != System
							.getProperty("ThinClientManager.server.Codebase"))
						try {
							homeServer = new URL(
									System.getProperty("ThinClientManager.server.Codebase"))
									.getHost();
						} catch (final MalformedURLException e1) {
							e1.printStackTrace();
						}
				} else {
					realm = (Realm) node.getLookup().lookup(Realm.class);
					if (null != realm.getSchemaProviderName())
						homeServer = realm.getSchemaProviderName();
					else if (null != realm.getConnectionDescriptor()
							.getHostname())
						homeServer = realm.getConnectionDescriptor()
								.getHostname();
				}
				if (homeServer == null || homeServer.length() == 0)
					homeServer = "localhost";

				try {
					final URL url = new URL("http", homeServer, 8080, fileName);
					final BufferedReader br = new BufferedReader(
							new InputStreamReader(url.openStream()));
					String line = null;
					while ((line = br.readLine()) != null) {
						if (logIpAddress == null) {
							if (line.contains(macAddress)) {
								Matcher matcher = IPADDRESS_PATTERN
										.matcher(line);
								while (matcher.find())
									logIpAddress = matcher.group();

							}
						}
					}
				} catch (final MalformedURLException e) {
					e.printStackTrace();
					ErrorManager.getDefault().notify(e);
				} catch (final IOException e) {
					e.printStackTrace();
					ErrorManager.getDefault().notify(e);
				}
				VNCController.openConnection(new String[] { "-port=5900", "-host=",
						logIpAddress, "-FullScreen=n0", "-ScalingFactor=80%" });
			} else {
				VNCController.openConnection(new String[] { "-port=5900", "-host=", ipAddress,
						"-FullScreen=no", "-ScalingFactor=80%" });
			}
		}
	}

}
