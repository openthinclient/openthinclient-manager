package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.manager.standalone.servlet.FileServiceServlet;
import org.openthinclient.service.common.home.ManagerHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class StaticFilesServiceConfiguration  {

  private static final Logger LOG = LoggerFactory.getLogger(StaticFilesServiceConfiguration.class);

  @Autowired
  ManagerHome managerHome;

  @Bean
  public ServletRegistrationBean filesServlet() {

    final ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();
    servletRegistrationBean.addUrlMappings("/openthinclient/files/*");

    final Path nfsRootPath = managerHome.getLocation().toPath().resolve("nfs").resolve("root").toAbsolutePath();
    servletRegistrationBean.setServlet(new FileServiceServlet(nfsRootPath.toFile()));
    return servletRegistrationBean;

  }

  /*
  FIXME it would be much better to use the spring based resource resolving. Due to limitations in the client, this is not going to work yet

  class ... extends WebMvcConfigurerAdapter

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {

    final ResourceHandlerRegistration registration = registry.addResourceHandler("/openthinclient/files/**");

    final Path nfsRootPath = managerHome.getLocation().toPath().resolve("nfs").resolve("root").toAbsolutePath();

    LOG.info("publishing " + nfsRootPath + " (public URL: /openthinclient/files)");

    registration.addResourceLocations("file://" + nfsRootPath.toString() + "/");

  }

  */
}
