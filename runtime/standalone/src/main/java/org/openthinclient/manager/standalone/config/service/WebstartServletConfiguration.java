package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.jnlp.servlet.JnlpDownloadServlet;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebstartServletConfiguration {

  @Bean
  public ServletRegistrationBean jnlpServlet() {
    final ServletRegistrationBean reg = new ServletRegistrationBean();
    reg.setServlet(new JnlpDownloadServlet());
    reg.setName("jnlpDownloadServlet");
    reg.addUrlMappings("*.jnlp", "*.jar");
    return reg;
  }

}
