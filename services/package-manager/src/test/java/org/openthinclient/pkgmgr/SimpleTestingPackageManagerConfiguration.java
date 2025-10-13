package org.openthinclient.pkgmgr;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SimpleTestingPackageManagerConfiguration {

  @Autowired
  PackageManagerConfiguration configuration;

  @Bean
  public PackageManager packageManager() {
    return new PackageManagerFactory(new PackageManagerDatabase(null, null, null), null, null).createPackageManager(configuration);
  }

}
