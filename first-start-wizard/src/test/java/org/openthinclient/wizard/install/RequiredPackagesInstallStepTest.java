package org.openthinclient.wizard.install;

import org.junit.Test;
import org.openthinclient.util.dpkg.DPKGPackage;
import org.openthinclient.util.dpkg.Package;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RequiredPackagesInstallStepTest {
  @Test
  public void testResolvePackages() throws Exception {

    List<org.openthinclient.util.dpkg.Package> installable = Arrays.asList(
            createPackage("base", "2.0-12"),
            createPackage("base", "2.0-27"),
            createPackage("base", "2.0-18")
    );

    final List<Optional<Package>> result = new RequiredPackagesInstallStep(null).resolvePackages(installable, Collections.singletonList("base"));

    assertEquals(1, result.size());

    assertTrue(result.get(0).isPresent());

    assertEquals("0:2.0-27", result.get(0).get().getVersion().toString());

  }

  private DPKGPackage createPackage(String name, String version) {
    final DPKGPackage pkg = new DPKGPackage();
    pkg.setName(name);
    pkg.setVersion(version);
    return pkg;
  }

}
