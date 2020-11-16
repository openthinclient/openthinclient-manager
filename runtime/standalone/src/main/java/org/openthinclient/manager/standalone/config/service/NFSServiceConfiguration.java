package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.service.nfs.NFSService;
import org.openthinclient.web.pkgmngr.event.PackageEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class NFSServiceConfiguration {
  private NFSService service;

  @Bean
  public NFSService nfsService() {
    service = new NFSService();
    return service;
  }

  @Scheduled(cron = "0 0 11 * * 1-5") // every weekday at 11:00 am
  public void restartNFSService() {
    if(service != null && service.isRunning()) {
      service.restartService();
    }
  }

  @EventListener
  public void onPackageEvent(PackageEvent ev) {
    restartNFSService();
  }
}
