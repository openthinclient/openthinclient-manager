package org.openthinclient.api.versioncheck;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.DownloadManagerFactory;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ManagerHome;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Checks for available Versions at given URL
 */
public class AvailableVersionChecker {

    private ManagerHome managerHome;

    public AvailableVersionChecker(ManagerHome managerHome) {
        this.managerHome = managerHome;
    }

    /**
     * Return an UpdateDescriptor
     * @param uri the URI to check for updates.xml
     * @return an UpdateDescriptor
     * @throws Exception if parsing or downloading fails
     */
    public UpdateDescriptor getVersion(URI uri) throws JAXBException, IOException {

        if (requiresHttpDownload(uri)) {

            final DownloadManager downloadManager = getDownloadManager();
            if (downloadManager == null) {
                throw new IllegalStateException("No download manager is currently available");
            }
            return downloadManager.download(uri, in -> read(in));

        } else {

            try (final InputStream in = uri.toURL().openStream()) {
                return read(in);
            }
        }
    }

    private UpdateDescriptor read(InputStream in) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(UpdateDescriptor.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return (UpdateDescriptor) jaxbUnmarshaller.unmarshal(in);
    }

    public DownloadManager getDownloadManager() {
        if (managerHome == null) {
            return null;
        }
        final PackageManagerConfiguration configuration = managerHome.getConfiguration(PackageManagerConfiguration.class);
        return DownloadManagerFactory.create(configuration.getProxyConfiguration());
    }

    public boolean requiresHttpDownload(URI uri) {
        return uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https");
    }
}
