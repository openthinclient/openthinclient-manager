package org.openthinclient.api.importer.versioncheck;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.api.versioncheck.AvailableVersionChecker;
import org.openthinclient.api.versioncheck.UpdateDescriptor;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.impl.HttpClientDownloadManager;
import org.openthinclient.progress.NoopProgressReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test check read udates.xml and evaluate version
 */
@RunWith(SpringRunner.class)
@Import({VersionCheckerTest.DownloadManagerConfiguration.class})
public class VersionCheckerTest {

    @Configuration
    public static class DownloadManagerConfiguration {
        @Bean
        @Scope(value = "singleton")
        public DownloadManager downloadManager() {
            return new HttpClientDownloadManager(null, "TestUserAgent");
        }
    }

    @Autowired
    DownloadManager downloadManager;

    @Test
    public void testReadVersion() throws URISyntaxException, JAXBException, IOException {

        AvailableVersionChecker avc = new AvailableVersionChecker(null, downloadManager);
        URL resource = VersionCheckerTest.class.getResource("/versioncheck/updates.xml");
        assertNotNull("Missing updates.xml for Test.", resource);
        UpdateDescriptor version = avc.getVersion(resource.toURI(), new NoopProgressReceiver());
        assertNotNull(version);
        assertEquals(4, version.getEntries().size());
        assertEquals("2.2.2", version.getNewVersion());
    }
}
