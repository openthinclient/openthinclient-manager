package org.openthinclient.wizard;

import static org.openthinclient.web.WebUtil.getServletMappingRoot;

import com.vaadin.spring.annotation.EnableVaadin;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.boot.annotation.EnableVaadinServlet;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.inventory.SystemInventory;
import org.openthinclient.advisor.inventory.SystemInventoryFactory;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.DatabaseConfiguration.DatabaseType;
import org.openthinclient.manager.util.installation.InstallationDirectoryUtil;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.model.DatabaseModel;
import org.openthinclient.wizard.model.DatabaseModel.MySQLConfiguration;
import org.openthinclient.wizard.model.DirectoryModel;
import org.openthinclient.wizard.model.NetworkConfigurationModel;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.openthinclient.wizard.ui.FirstStartWizardUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableVaadin
@EnableVaadinServlet
@Import({EmbeddedServletContainerAutoConfiguration.class, WizardApplicationConfiguration.MinimalWebMvcConfiguration.class})
@PropertySource("classpath:/application.properties")
public class WizardApplicationConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(WizardApplicationConfiguration.class);

  @Autowired
  ApplicationContext applicationContext;

   @Value("${vaadin.servlet.urlMapping}")
   private String vaadinServletUrlMapping;
   
   @Value("${otc.manager.installation.freespace.minimum}")
   private int installationFreespaceMinimum;   
   
  /**
   * The only purpose of this filter is to redirect root URL requests to the first start wizard. This will ensure that any
   * potential index.html on the classpath will not be preferred.
   *
   * @return the filter configuration
   */
  @Bean
  public FilterRegistrationBean redirectToWizardFilter() {
    final FilterRegistrationBean redirectFilter = new FilterRegistrationBean();
    // handle the root request only
    redirectFilter.addUrlPatterns("/");
    redirectFilter.setFilter(new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        response.sendRedirect(getServletMappingRoot(vaadinServletUrlMapping) + "first-start");
      }
    });
    return redirectFilter;
  }

  @Bean
  public AsyncListenableTaskExecutor taskExecutor() {
    final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setMaxPoolSize(2);
    return taskExecutor;
  }

  @Bean
  public CheckExecutionEngine checkExecutionEngine(AsyncListenableTaskExecutor taskExecutor) {
    return new CheckExecutionEngine(taskExecutor);
  }

  @Bean
  public SystemInventoryFactory systemInventoryFactory() {
    return new SystemInventoryFactory(taskExecutor());
  }

  @Bean
  public FactoryBean<SystemInventory> systemInventoryFactoryBean() {

    final SystemInventoryFactory systemInventoryFactory = systemInventoryFactory();
    final ListenableFuture<SystemInventory> systemInventoryFuture = systemInventoryFactory.determineSystemInventory();

    return new SystemInventoryFactoryBean(systemInventoryFuture);

  }
  
  @Bean
  public ManagerHomeFactory managerHomeFactory() {
     return new ManagerHomeFactory();
  }

  @Bean
  public SystemSetupModel systemSetupModel(ManagerHomeFactory managerHomeFactory, SystemInventory systemInventory,
                                           CheckExecutionEngine checkExecutionEngine, AsyncListenableTaskExecutor taskExecutor) {

    SystemSetupModel model = new SystemSetupModel(managerHomeFactory, systemInventory, checkExecutionEngine, applicationContext, taskExecutor, installationFreespaceMinimum); //

    // check if there is an broken installation and try to restore already set up properties
    if (InstallationDirectoryUtil.existsInstallationProgressFile(managerHomeFactory.getManagerHomeDirectory())) {
      LOG.info("Found existing installation file, try to read already setup properties.");

      ManagerHome managerHome = managerHomeFactory.create();

      PackageManagerConfiguration packageManagerConfiguration = managerHome.getConfiguration(PackageManagerConfiguration.class); //
      NetworkConfigurationModel networkConfigurationModel = model.getNetworkConfigurationModel();
      if (packageManagerConfiguration.getProxyConfiguration() != null) {
        LOG.info("Restore previous setup proxy settings.");
        networkConfigurationModel.getProxyConfiguration().setEnabled(true);
        networkConfigurationModel.getProxyConfiguration().setPort(packageManagerConfiguration.getProxyConfiguration().getPort());
        networkConfigurationModel.getProxyConfiguration().setHost(packageManagerConfiguration.getProxyConfiguration().getHost());
        networkConfigurationModel.getProxyConfiguration().setUser(packageManagerConfiguration.getProxyConfiguration().getUser());
        networkConfigurationModel.getProxyConfiguration().setPassword(packageManagerConfiguration.getProxyConfiguration().getPassword());
      } else {
        LOG.info("No proxy settings found, using defaults.");
      }

      DatabaseConfiguration databaseConfiguration = managerHome.getConfiguration(DatabaseConfiguration.class);
      DatabaseModel databaseModel = model.getDatabaseModel();
      if (databaseConfiguration != null && databaseConfiguration.getType() != null) {
        databaseModel.setType(databaseConfiguration.getType());
        if (databaseConfiguration.getType() == DatabaseType.MYSQL) {
          MySQLConfiguration mySQLConfiguration = databaseModel.getMySQLConfiguration();
          String url = databaseConfiguration.getUrl();
          try {
            URI uri = new URI(url.substring(5));
            mySQLConfiguration.setDatabase(uri.getPath().substring(1));
            mySQLConfiguration.setHostname(uri.getHost());
            mySQLConfiguration.setPort(uri.getPort());
          } catch (URISyntaxException e) {
            LOG.error("Cannot parse database uri, using defaults.");
          }
          mySQLConfiguration.setUsername(databaseConfiguration.getUsername());
          mySQLConfiguration.setPassword(databaseConfiguration.getPassword());
        }
      } else {
        LOG.info("No database settings found, using defaults.");
      }

      DirectoryServiceConfiguration directoryServiceConfiguration = managerHome.getConfiguration(DirectoryServiceConfiguration.class);
      DirectoryModel directoryModel = model.getDirectoryModel();
      if (directoryServiceConfiguration != null) {
        OrganizationalUnit primaryOU = directoryModel.getPrimaryOU();
        primaryOU.setName(directoryServiceConfiguration.getPrimaryOU());
      } else {
        LOG.info("No directory settings found, using defaults.");
      }
    }

    return model;
  }

  @Bean
  @UIScope
  public FirstStartWizardUI firstStartWizardUI() {
    return new FirstStartWizardUI();
  }

  private static class SystemInventoryFactoryBean extends AbstractFactoryBean<SystemInventory> {

    private final ListenableFuture<SystemInventory> systemInventoryFuture;
    private SystemInventory systemInventory;

    public SystemInventoryFactoryBean(ListenableFuture<SystemInventory> systemInventoryFuture) {
      setSingleton(true);
      this.systemInventoryFuture = systemInventoryFuture;
    }

    @Override
    public Class<?> getObjectType() {
      return SystemInventory.class;
    }

    @Override
    protected SystemInventory createInstance() throws Exception {
      if (systemInventory == null) {
        systemInventory = systemInventoryFuture.get();
      }
      return systemInventory;
    }
  }
  

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfig() {
      return new PropertySourcesPlaceholderConfigurer();
  }

  /**
   * A minimalistic spring web mvc configuration, allowing to serve static resources.
   * As the first start wizard will not bootstrap a spring boot application entirely, this configuration
   * will take care of setting up a very simple {@link DispatcherServlet} configuration that will serve
   * contents from the classpath.
   */
  @Configuration
  @EnableWebMvc
  public static class MinimalWebMvcConfiguration {
    @Autowired
    WebApplicationContext applicationContext;

    @Bean
    public ServletRegistrationBean dispatcherRegistration() {
      final ServletRegistrationBean reg = new ServletRegistrationBean();
      reg.addUrlMappings("/");
      reg.setServlet(new DispatcherServlet(applicationContext));
      return reg;
    }

    @Bean
    public WebMvcConfigurerAdapter staticResourcesConfigurer() {
      return new WebMvcConfigurerAdapter() {
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {

          registry.addResourceHandler("/**")
                  .addResourceLocations(
                          "classpath:/public/");

        }
      };
    }

  }
}
