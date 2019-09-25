package org.openthinclient.wizard.install;

import org.junit.Test;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.api.distributions.InstallableDistributions;
import org.openthinclient.pkgmgr.db.Source;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.bind.Marshaller;
import java.net.URL;

import static org.junit.Assert.*;

public class InstallableDistributionsTest {

  static final String EXPECTED_01 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + //
          "<distributions>\n" + //
          "    <distribution name=\"Dist1\" preferred=\"false\">\n" + //
          "        <description>Description 1</description>\n" + //
          "        <sources>\n" + //
          "            <source enabled=\"true\" default=\"false\">\n" + //
          "                <description>openthinclient.org Pales</description>\n" + //
          "                <url>http://archive.openthinclient.org/openthinclient/v2.1/manager-rolling/</url>\n" + //
          "            </source>\n" + //
          "            <source enabled=\"true\" default=\"false\">\n" + //
          "                <description>openthinclient.org Pales Testing</description>\n" + //
          "                <url>http://archive.openthinclient.org/openthinclient/v2.1/manager-test/</url>\n" + //
          "            </source>\n" + //
          "        </sources>\n" + //
          "        <install-package>base</install-package>\n" + //
          "    </distribution>\n" + //
          "</distributions>";


  @Test
  public void testSerialize() throws Exception {

    final InstallableDistributions distributions = new InstallableDistributions();

    final InstallableDistribution id = new InstallableDistribution("Dist1", "Description 1");
    final Source source1 = new Source();
    source1.setEnabled(true);
    source1.setUrl(new URL("http://archive.openthinclient.org/openthinclient/v2.1/manager-rolling/"));
    source1.setDescription("openthinclient.org Pales");
    id.getSourcesList().getSources().add(source1);
    final Source source2 = new Source();
    source2.setEnabled(true);
    source2.setUrl(new URL("http://archive.openthinclient.org/openthinclient/v2.1/manager-test/"));
    source2.setDescription("openthinclient.org Pales Testing");
    id.getSourcesList().getSources().add(source2);

    id.getMinimumPackages().add("base");
    distributions.getInstallableDistributions().add(id);


    final Marshaller marshaller = InstallableDistributions.CONTEXT.createMarshaller();

    final javax.xml.transform.Source expected = Input.fromString(EXPECTED_01).build();
    final javax.xml.transform.Source actual = Input.fromJaxb(distributions).withMarshaller(marshaller).build();

    final Diff diff = DiffBuilder
            .compare(expected)
            .withTest(actual)
            .ignoreWhitespace()
            .build();

    assertFalse("Serialized model differs: " + diff.toString(), diff.hasDifferences());
  }

  @Test
  public void testReadDefaultDistributionsXml() throws Exception {

    final InstallableDistributions defaultDistributions = InstallableDistributions.getDefaultDistributions();

    assertNotNull(defaultDistributions);

    assertEquals(1, defaultDistributions.getInstallableDistributions().size());
    final InstallableDistribution distributionPales = defaultDistributions.getInstallableDistributions().get(0);

    assertEquals(new URL("http://archive.openthinclient.org/openthinclient/v2.1/manager-rolling/"), distributionPales.getSourcesList().getSources().get(0).getUrl());
    assertTrue("Pales distribution enabled", distributionPales.getSourcesList().getSources().get(0).isEnabled());
    assertEquals("base", distributionPales.getMinimumPackages().get(0));
    assertTrue(distributionPales.isPreferred());

  }

  /**
   * This test will ensure that a default preferred distribution is available. If not, other logic
   * will fail
   */
  @Test
  public void testDefaultDistributionIsAvailable() throws Exception {

    assertNotNull(InstallableDistributions.getPreferredDistribution());

  }
}