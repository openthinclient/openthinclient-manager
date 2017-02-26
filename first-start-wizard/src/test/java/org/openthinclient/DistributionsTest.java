package org.openthinclient;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.wizard.install.ImportItem;
import org.openthinclient.wizard.install.ImportableProfileProvider;
import org.openthinclient.wizard.install.InstallContext;
import org.openthinclient.wizard.install.InstallableDistribution;
import org.openthinclient.wizard.install.InstallableDistributions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

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
      final AbstractProfileObject obj = provider.access(new InstallContext(), importItem);

      assertNotNull("Failed to resolve import item " + importItem, obj);

    }




  }
}
