package org.openthinclient.pkgmgr.spring;

import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PackageManagerExecutionEngineConfiguration {

  @Bean
  ThreadPoolTaskExecutor packageManagerTaskExecutor() {
    // the package manager does not allow more than one concurrent operation.
    final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(1);
    threadPoolTaskExecutor.setMaxPoolSize(1);
    return threadPoolTaskExecutor;
  }

  @Bean
  public PackageManagerExecutionEngine executionEngine() {
    return new PackageManagerExecutionEngine(packageManagerTaskExecutor());
  }

}
