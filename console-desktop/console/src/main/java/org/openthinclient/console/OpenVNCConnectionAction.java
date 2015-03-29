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

	/*
	 * This pattern is used to find a matching IP-Address in the Client-log.
	 */
	private static final Pattern IPADDRESS_PATTERN = Pattern
			.compile("([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
					+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
					+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
					+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])");
	/*
	 * This is the pathname on the server, where the client-log-file is located.
	 */
	private static String fileName = "/openthinclient/files/var/log/syslog.log";

	/*
	 * @see org.openide.util.actions.CallableSystemAction#asynchronous()
	 * This action should be performed asynchronously in a private thread.
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
	 * This method returns the action-name of this class, in a human presentable  form, 
	 * based on a key, which is the name of the class it self. 
	 * You can find the combined keys at 
	 * console.main.resources.org.openthinclient.console.messages.properties#action.
	 * Or for the German messages: 
	 * console.main.resources.org.openthinclient.console.messages_de.properties#action.
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

	/*
	 * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 * In this method we are trying to start a VNC-connection to a client. 
	 * To realize that, we have to get the client IP address.
	 * At first we have to know the IP address from the server. 
	 * Than we have to locate the client-log-file on it. If we have located it,
	 * we can search for the known MAC-Address in it.
	 * After that, we use our pattern IPADDRESS_PATTERN to find the IP address.
	 * To be sure that this the newest IP address for the client, we take the last one out of the list.
	 * If there is no IP address, we inform the user about this by showing a message-dialog with a notification.
	 * Now we are finally able to start a VNC-connection by calling the method openConnection 
	 * from the class VNCController.
	 */
	@SuppressWarnings({ "deprecation", "unused" })
	@Override
	protected void performAction(Node[] nodes) {
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

	/*
	 * This method iterates the client-set by the given nodes, to get the correct client.
	 */
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

	/*
	 * This method checks if the selected node is a instance of the type Client.
	 * If its true, the method returns it.
	 */
	private Client toClient(final Node node) {
		DirectoryObject client = (DirectoryObject) node.getLookup().lookup(
				DirectoryObject.class);

		if (client instanceof Client)
			return (Client) client;

		return null;
	}

}
