package org.openthinclient.console;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenVNCConnectionAction extends NodeAction {

	private static final long serialVersionUID = 1L;

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
	protected boolean enable(Node[] nodes) {
		return getClients(nodes).iterator().hasNext();
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
	protected void performAction(Node[] nodes) throws NullPointerException {
		for (final Client client : getClients(nodes)) {

			String macAddress = client.getMacAddress();
			String logIpAddress = null;
			String homeServer = "";
			Realm realm = (Realm) nodes[0].getLookup().lookup(Realm.class);
			if (null != realm.getSchemaProviderName())
				homeServer = realm.getSchemaProviderName();
			else if (null != realm.getConnectionDescriptor().getHostname())
				homeServer = realm.getConnectionDescriptor().getHostname();
			if (homeServer == null || homeServer.length() == 0)
				homeServer = "localhost";

			try {
				final URL url = new URL("http", homeServer, 8080, fileName);
				final BufferedReader br = new BufferedReader(
						new InputStreamReader(url.openStream()));
				String line = null;
				while ((line = br.readLine()) != null) {
					if (line.contains(macAddress)) {
						Matcher matcher = IPADDRESS_PATTERN.matcher(line);
						while (matcher.find())
							logIpAddress = matcher.group();
					}
				}
			} catch (final MalformedURLException e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
			} catch (final IOException e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
			}
			if (logIpAddress == null) {
				Component frame = null;
				JOptionPane.showMessageDialog(frame, Messages
						.getString("OpenVNCConnectionNullPointerException"));
			} else {
				VNCController.openConnection(new String[] { "-port=5900",
						"-host=", logIpAddress, "-FullScreen=NO"});
			}
		}
	}

	private Iterable<Client> getClients(Node[] nodes) {

		if (nodes == null || nodes.length == 0)
			return Collections.emptyList();

		final List<Client> clients = new ArrayList<Client>();

		for (Node n : nodes) {
			Client client = toClient(n);

			if (client != null)
				clients.add(client);
		}
		return clients;
	}

	private Client toClient(final Node node) {
		DirectoryObject client = (DirectoryObject) node.getLookup().lookup(
				DirectoryObject.class);

		if (client instanceof Client)
			return (Client) client;

		return null;
	}

}
