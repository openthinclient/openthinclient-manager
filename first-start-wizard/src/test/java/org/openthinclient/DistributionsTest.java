package org.openthinclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.openthinclient.api.context.InstallContext;
import org.openthinclient.api.distributions.ImportItem;
import org.openthinclient.api.distributions.ImportableProfileProvider;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.api.distributions.InstallableDistributions;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.progress.LoggingProgressReceiver;

public class DistributionsTest {

  @Test
  public void testParentCorrectlySet() throws Exception {
    final InstallableDistributions defaultDistributions = InstallableDistributions.getDefaultDistributions();

    assertSame(defaultDistributions, defaultDistributions.getInstallableDistributions().get(0).getParent());

  }

  @Test
  public void testLoadDistributionsXML() throws Exception {

    final InstallableDistributions defaultDistributions = InstallableDistributions.getDefaultDistributions();

    assertNotNull(defaultDistributions);

    assertEquals(1, defaultDistributions.getInstallableDistributions().size());

    assertThat(defaultDistributions.getBaseURI().toString(), CoreMatchers.endsWith("org/openthinclient/"));

  }

  @Test
  public void testImportStepsResolvable() throws Exception {

    final InstallableDistributions defaultDistributions = InstallableDistributions.getDefaultDistributions();

    final ImportableProfileProvider provider = new ImportableProfileProvider(defaultDistributions.getBaseURI());

    final InstallableDistribution distribution = defaultDistributions.getPreferred();

    for (ImportItem importItem : distribution.getImportItems()) {
      final AbstractProfileObject obj = provider.access(new InstallContext(), importItem, new LoggingProgressReceiver());
      assertNotNull("Failed to resolve import item " + importItem, obj);
    }
  }
}
