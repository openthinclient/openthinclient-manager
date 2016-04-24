package org.openthinclient.web;

import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.web.filter.OncePerRequestFilter;
import org.vaadin.spring.http.HttpService;
import org.vaadin.spring.security.annotation.EnableVaadinSharedSecurity;
import org.vaadin.spring.security.config.VaadinSharedSecurityConfiguration;
import org.vaadin.spring.security.shared.VaadinAuthenticationSuccessHandler;
import org.vaadin.spring.security.shared.VaadinUrlAuthenticationSuccessHandler;
import org.vaadin.spring.security.web.VaadinRedirectStrategy;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.openthinclient.web.WebUtil.getServletMappingRoot;

/**
 * Configure Spring Security.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, proxyTargetClass = true)
@EnableVaadinSharedSecurity
public class WebApplicationSecurityConfiguration extends WebSecurityConfigurerAdapter {

   private static final Logger LOG = LoggerFactory.getLogger(WebApplicationSecurityConfiguration.class);

   @Autowired
   private ManagerHome managerHome;

   @Value("${vaadin.servlet.urlMapping}")
   private String vaadinServletUrlMapping;
   
   /**
    * The only purpose of this filter is to redirect root URL requests to the first start wizard. This will ensure that any
    * potential index.html on the classpath will not be preferred.
    *
    * @return the filter configuration
    */
   @Bean
   public FilterRegistrationBean redirectToDashboardUIFilter() {
     final FilterRegistrationBean redirectFilter = new FilterRegistrationBean();
     // handle the root request only
     redirectFilter.addUrlPatterns("/");
     redirectFilter.setFilter(new OncePerRequestFilter() {
       @Override
       protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
         response.sendRedirect(getServletMappingRoot(vaadinServletUrlMapping));
       }
     });
     return redirectFilter;
   } 
   
   @Override
   public void configure(AuthenticationManagerBuilder auth) throws Exception {

      DirectoryServiceConfiguration dsc = managerHome.getConfiguration(DirectoryServiceConfiguration.class);
      // FIXME localhost should not be hardcoded here!
      String ldapUrl = "ldap://localhost:" + dsc.getEmbeddedLdapPort() + "/ou=" + dsc.getPrimaryOU() + "," + dsc.getEmbeddedCustomRootPartitionName();

      LOG.info("Configuring authentication for LDAP: {}", ldapUrl);

      final LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> ldapAuthBuilder = auth.ldapAuthentication();

      ldapAuthBuilder.contextSource() //
            .url(ldapUrl) //
            .managerDn(dsc.getContextSecurityPrincipal()) //
            .managerPassword(dsc.getContextSecurityCredentials());

      ldapAuthBuilder.userDnPatterns("cn={0},ou=users")
            .contextSource();
   }

   @Override
   protected void configure(HttpSecurity http) throws Exception {
      http.csrf().disable(); // Use Vaadin's built-in CSRF protection instead

      // Read the vaadin.servlet.urlMapping property
      String urlMapping = getServletMappingRoot(vaadinServletUrlMapping);
      
      // @formatter:off
      http.authorizeRequests()
              .anyRequest().permitAll();

      http.httpBasic().disable();
      http.formLogin().disable();
      http.logout()
              .logoutUrl(urlMapping + "logout")
              .logoutSuccessUrl(urlMapping)
              .permitAll();
      http.rememberMe().rememberMeServices(rememberMeServices()).key("openthinclient-manager");
      // @formatter:on
   }

   @Override
   public void configure(WebSecurity web) throws Exception {
      web.ignoring().antMatchers("/VAADIN/**");
   }

   @Override
   @Bean
   public AuthenticationManager authenticationManagerBean() throws Exception {
      return super.authenticationManagerBean();
   }

   @Bean
   public RememberMeServices rememberMeServices() {
      // TODO Is there some way of exposing the RememberMeServices instance that the remember me configurer creates by default?
      TokenBasedRememberMeServices services = new TokenBasedRememberMeServices("openthinclient-manager", userDetailsService());
      services.setAlwaysRemember(true);
      return services;
   }

   @Bean(name = VaadinSharedSecurityConfiguration.VAADIN_AUTHENTICATION_SUCCESS_HANDLER_BEAN)
   VaadinAuthenticationSuccessHandler vaadinAuthenticationSuccessHandler(HttpService httpService, VaadinRedirectStrategy vaadinRedirectStrategy) {
      return new VaadinUrlAuthenticationSuccessHandler(httpService, vaadinRedirectStrategy, getServletMappingRoot(vaadinServletUrlMapping));
   }
}
