package org.openthinclient.wizard.install;

import org.junit.Test;
import org.openthinclient.api.distributions.ImportItem;
import org.openthinclient.api.distributions.ImportableProfileProvider;
import org.openthinclient.api.distributions.InstallableDistributions;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImportableProfileProviderTest {

  @Test
  public void testResolveOnlineURI() throws Exception {

    final ImportableProfileProvider provider = new ImportableProfileProvider(new URI("http://archive.openthinclient.org/openthinclient/"));

    assertEquals(new URI("http://archive.openthinclient.org/openthinclient/profiles/current/client-default.json"), provider.createTargetURI(new ImportItem.Client("profiles/current/client-default.json")));
    assertEquals(new URI("http://archive.openthinclient.org/profiles/current/client-default.json"), provider.createTargetURI(new ImportItem.Client("../profiles/current/client-default.json")));

  }

  @Test
  public void testRequiresHttpDownload() throws Exception {
    ImportableProfileProvider provider = new ImportableProfileProvider(InstallableDistributions.getDefaultDistributionsURL().toURI());
    assertFalse(provider.requiresHttpDownload(provider.createTargetURI(new ImportItem.Client("profiles/current/client-default.json"))));

    provider = new ImportableProfileProvider(URI.create("http://example.com/distribution.xml"));
    assertTrue(provider.requiresHttpDownload(provider.createTargetURI(new ImportItem.Client("profiles/current/client-default.json"))));

    provider = new ImportableProfileProvider(URI.create("https://example.com/distribution.xml"));
    assertTrue(provider.requiresHttpDownload(provider.createTargetURI(new ImportItem.Client("profiles/current/client-default.json"))));
  }
}