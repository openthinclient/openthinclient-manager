package org.openthinclient.web.app;

import org.openthinclient.web.app.conf.PackageManagerUIConfiguration;
import org.openthinclient.web.pkgmngr.ui.PackageManagerTestUI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(PackageManagerUIConfiguration.class)
public class PackageManagerTestApplication {

  @Bean
  public PackageManagerTestUI packageManagerTestUI() {
    return new PackageManagerTestUI();
  }

  public static void main(String[] args) {

    SpringApplication.run(PackageManagerTestApplication.class, args);

  }
}
