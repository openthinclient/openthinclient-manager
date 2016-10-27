package org.openthinclient.console;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeEvent;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.Preferences;

import javax.security.auth.callback.CallbackHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.util.UsernamePasswordCallbackHandler;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.LDAPConnectionDescriptor.AuthenticationMethod;
import org.openthinclient.ldap.LDAPConnectionDescriptor.ConnectionMethod;
import org.openthinclient.ldap.LDAPConnectionDescriptor.ProviderType;

/**
 * Static utility class used to manage the set of registered realms in a central
 * location.
 */
public class RealmManager {
	
	private static final Preferences prefs = ConsoleFrame.PREFERENCES_ROOT.node("realms");

	public static String[] getRegisteredRealmNames() throws BackingStoreException {
		return prefs.childrenNames();
	}

	public static Realm loadRealm(String realmName) throws BackingStoreException,
			IOException, ClassNotFoundException, DirectoryException {
		return new Realm(fromPreferences(prefs.node(realmName)));
	}

	public static void registerRealm(Realm realm) {
		// fix callback handler to use the correct protection domain
		final CallbackHandler callbackHandler = realm.getConnectionDescriptor()
				.getCallbackHandler();
		if (callbackHandler instanceof UsernamePasswordCallbackHandler)
			try {
				((UsernamePasswordCallbackHandler) callbackHandler)
						.setProtectionDomain(realm.getConnectionDescriptor().getLDAPUrl());
			} catch (final IOException e) {
				ErrorManager.getDefault().annotate(e,
						"Could not update protection domain.");
				ErrorManager.getDefault().notify(e);
			}
		else
			ErrorManager.getDefault().notify(
					new IOException("CallbackHandler was not of the expected type, but "
							+ callbackHandler.getClass()));

		try {
			final String baseName = realm.getConnectionDescriptor().getHostname()
					+ realm.getConnectionDescriptor().getBaseDN(); //$NON-NLS-1$
			if (prefs.nodeExists(baseName)) { //$NON-NLS-1$
				DialogDisplayer.getDefault().notify(
						new NotifyDescriptor.Message(Messages
								.getString("error.RealmAlreadyExists"), //$NON-NLS-1$
								NotifyDescriptor.WARNING_MESSAGE));
				return;
			}

			final Preferences realmPrefs = prefs.node(baseName);
			toPreferences(realmPrefs, realm.getConnectionDescriptor());
			prefs.flush();
		} catch (final Exception ioe) {
			ErrorManager.getDefault().notify(ioe);
		}
	}

	public static void deregisterRealm(String realmName) throws BackingStoreException {
		prefs.node(realmName).removeNode();
		prefs.flush();
	}

	private static EventListenerList listeners = new EventListenerList();

	static {
		prefs.addNodeChangeListener(new NodeChangeListener() {
			public void childRemoved(NodeChangeEvent nodechangeevent) {
				notify(nodechangeevent);
			}

			public void childAdded(NodeChangeEvent nodechangeevent) {
				notify(nodechangeevent);
			}

			private void notify(NodeChangeEvent nodechangeevent) {
				if (listeners.getListenerCount() > 0) {
					final ChangeEvent e = new ChangeEvent(nodechangeevent);
					for (final ChangeListener l : listeners
							.getListeners(ChangeListener.class))
						l.stateChanged(e);
				}
			}
		});
	}

	public static void addChangeListener(final ChangeListener changeListener) {
		listeners.add(ChangeListener.class, changeListener);
	}

	public static void removeChangeListener(ChangeListener listener) {
		listeners.remove(ChangeListener.class, listener);
	}

	/**
	 * Make an {@link LDAPConnectionDescriptor} from {@link Preferences}.
	 * 
	 * @param p the node where preferences will be stored.
	 * @return
	 * @throws BackingStoreException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private static LDAPConnectionDescriptor fromPreferences(Preferences p)
			throws BackingStoreException, IOException, ClassNotFoundException {
		final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();

		lcd.setAuthenticationMethod(AuthenticationMethod.valueOf(p.get("authentication method", AuthenticationMethod.NONE.name())));
		lcd.setBaseDN(p.get("base DN", ""));
		lcd.setConnectionMethod(ConnectionMethod.valueOf(p.get("connection method", ConnectionMethod.PLAIN.name())));
		lcd.setHostname(p.get("hostname", "localhost"));
		lcd.setPortNumber((short) p.getInt("port", 10389));
		lcd.setProviderType(ProviderType.valueOf(p.get("provider type", ProviderType.SUN.name())));

		// add extra environment parameters
		if (p.nodeExists("env")) {
			final Preferences env = p.node("env");
			for (final String name : env.keys()) {
				final String v = p.get(name, null);

				Object value = null;
				if (v != null && v.length() != 0)
					switch (v.charAt(0)){
						case 'I' : // integer
							value = Integer.parseInt(v.substring(1));
							break;
						case 'S' : // string
							value = v.substring(1);
							break;
						case 'O' : // object
							final byte[] b = p.getByteArray(name, null);
							final ObjectInputStream ois = new ObjectInputStream(
									new ByteArrayInputStream(b, 1, b.length));
							value = ois.readObject();
							ois.close();
							break;
					}

				lcd.getExtraEnv().put(name, value);
			}
		}

		lcd.setCallbackHandler(new UsernamePasswordCallbackHandler(lcd.getLDAPUrl()));

		return lcd;
	}

	/**
	 * Store {@link LDAPConnectionDescriptor} settings in the given
	 * {@link Preferences} node.
	 * 
	 * @param p
	 * @param lcd
	 * @throws IOException
	 */
	private static void toPreferences(Preferences p, LDAPConnectionDescriptor lcd)
			throws IOException {
		p.put("authentication method", lcd.getAuthenticationMethod().name());
		p.put("base DN", lcd.getBaseDN());
		p.put("connection method", lcd.getConnectionMethod().name());
		p.put("hostname", lcd.getHostname());
		p.putInt("port", lcd.getPortNumber());
		p.put("provider type", lcd.getProviderType().name());

		final Preferences env = p.node("env");
		for (final Map.Entry<String, Object> e : lcd.getExtraEnv().entrySet())
			if (e.getValue() instanceof String)
				env.put(e.getKey(), (String) e.getValue());
			else if (e.getValue() instanceof Integer)
				env.putInt(e.getKey(), (Integer) e.getValue());
			else {
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(e.getValue());
				oos.flush();

				env.putByteArray(e.getKey(), baos.toByteArray());
			}
	}
}
