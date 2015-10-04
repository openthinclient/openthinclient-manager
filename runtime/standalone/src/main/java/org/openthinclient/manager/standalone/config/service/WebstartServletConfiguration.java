package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.jnlp.servlet.JnlpDownloadServlet;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class WebstartServletConfiguration {

  @Bean
  @Order(10)
  public ServletRegistrationBean jnlpServlet() {

    final ServletRegistrationBean reg = new ServletRegistrationBean();

    reg.setServlet(new JnlpDownloadServlet());

    reg.addUrlMappings("/console/*.jnlp");
    reg.addUrlMappings("console/*.jar");

    return reg;
  }

}
