package org.openthinclient.console;

import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.security.auth.callback.CallbackHandler;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.util.UsernamePasswordCallbackHandler;

/**
 * Static utility class used to manage the set of registered realms in a central
 * location.
 */
public class RealmManager {
	private static DataFolder folder;

	private static DataFolder getDataFolder() throws DataObjectNotFoundException {
		if (null == folder)
			folder = (DataFolder) DataObject.find(Repository.getDefault()
					.getDefaultFileSystem().getRoot().getFileObject("Realms")); //$NON-NLS-1$

		return folder;
	}

	public static void registerRealm(Realm realm) {
		// fix callback handler to use the correct protection domain
		CallbackHandler callbackHandler = realm.getConnectionDescriptor()
				.getCallbackHandler();
		if (callbackHandler instanceof UsernamePasswordCallbackHandler)
			try {
				((UsernamePasswordCallbackHandler) callbackHandler)
						.setProtectionDomain(realm.getConnectionDescriptor().getLDAPUrl());
			} catch (IOException e) {
				ErrorManager.getDefault().annotate(e,
						"Could not update protection domain.");
				ErrorManager.getDefault().notify(e);
			}
		else
			ErrorManager.getDefault().notify(
					new IOException("CallbackHandler was not of the expected type, but "
							+ callbackHandler.getClass()));

		try {
			FileObject fld = getDataFolder().getPrimaryFile();
			String baseName = "realm-" //$NON-NLS-1$
					+ realm.getConnectionDescriptor().getHostname()
					+ realm.getConnectionDescriptor().getBaseDN(); //$NON-NLS-1$
			if (fld.getFileObject(baseName, "ser") != null) { //$NON-NLS-1$
				DialogDisplayer.getDefault().notify(
						new NotifyDescriptor.Message(Messages
								.getString("error.RealmAlreadyExists"), //$NON-NLS-1$
								NotifyDescriptor.WARNING_MESSAGE));
				return;
			}

			FileObject writeTo = fld.createData(baseName, "ser"); //$NON-NLS-1$
			FileLock lock = writeTo.lock();
			try {
				ObjectOutputStream str = new ObjectOutputStream(writeTo
						.getOutputStream(lock));
				try {

					str.writeObject(realm);

				} finally {
					str.close();
				}
			} finally {
				lock.releaseLock();
			}
		} catch (IOException ioe) {
			ErrorManager.getDefault().notify(ioe);
		}
	}

}
