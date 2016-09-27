package org.openthinclient.wizard;

import static org.openthinclient.web.WebUtil.getServletMappingRoot;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.inventory.SystemInventory;
import org.openthinclient.advisor.inventory.SystemInventoryFactory;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.openthinclient.wizard.ui.FirstStartWizardUI;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vaadin.spring.annotation.EnableVaadin;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.spring.boot.annotation.EnableVaadinServlet;

@Configuration
@EnableVaadin
@EnableVaadinServlet
@Import({EmbeddedServletContainerAutoConfiguration.class})
@PropertySource("classpath:/application.properties")
public class WizardApplicationConfiguration {

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
  public SystemSetupModel systemSetupModel(SystemInventory systemInventory, CheckExecutionEngine checkExecutionEngine, AsyncListenableTaskExecutor taskExecutor) {
    return new SystemSetupModel(systemInventory, checkExecutionEngine, applicationContext, taskExecutor, installationFreespaceMinimum);
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
  
}
