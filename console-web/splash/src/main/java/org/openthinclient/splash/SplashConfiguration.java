package org.openthinclient.splash;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SplashConfiguration {
  @Bean
  public ProgressBeanPostProcessor progressBeanPostProcessor() {
    return new ProgressBeanPostProcessor();
  }
}
