import org.junit.Test;
import org.openthinclient.DownloadManagerFactory;
import org.openthinclient.manager.util.http.impl.HttpClientDownloadManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * DownloadManagerFactoryTest
 */
public class DownloadManagerFactoryTest {

    @Test
    public void testReadPomVersion() {
        String serverID = "serverID";
        HttpClientDownloadManager downloadManager = (HttpClientDownloadManager) DownloadManagerFactory.create(serverID, null);
        assertNotNull(downloadManager);
        assertEquals("Unexpected serverID", serverID + "-0.0.0-TEST", downloadManager.getUserAgent());
    }
}
