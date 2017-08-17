package org.openthinclient.api.importer.versioncheck;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.api.versioncheck.AvailableVersionChecker;
import org.openthinclient.api.versioncheck.UpdateDescriptor;
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
public class VersionCheckerTest {

    @Test
    public void testReadVersion() throws URISyntaxException, JAXBException, IOException {

        AvailableVersionChecker avc = new AvailableVersionChecker(null);
        URL resource = VersionCheckerTest.class.getResource("/versioncheck/updates.xml");
        assertNotNull("Missing updates.xml for Test.", resource);
        UpdateDescriptor version = avc.getVersion(resource.toURI());
        assertNotNull(version);
        assertEquals(4, version.getEntries().size());
        assertEquals("2.2.2", version.getNewVersion());
    }
}
