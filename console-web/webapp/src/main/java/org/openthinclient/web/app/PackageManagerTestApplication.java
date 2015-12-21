package org.openthinclient.web.app;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.web.app.conf.PackageManagerUIConfiguration;
import org.openthinclient.web.pkgmngr.ui.PackageManagerTestUI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@SpringBootApplication
@Import(PackageManagerUIConfiguration.class)
public class PackageManagerTestApplication {

  @Bean
  @Scope("prototype")
  public PackageManagerTestUI packageManagerTestUI() {
    return new PackageManagerTestUI();
  }

  @Bean
  public PackageManager packageManager() {
    // only for testing. runtime standalone has a more sophisticated setup
    return PackageManagerFactory.createPackageManager(new ManagerHomeFactory().create().getConfiguration(PackageManagerConfiguration.class));
  }

  public static void main(String[] args) {

    SpringApplication.run(PackageManagerTestApplication.class, args);

  }
}
