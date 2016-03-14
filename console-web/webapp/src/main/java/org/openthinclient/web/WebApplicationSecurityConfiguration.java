package org.openthinclient.web;

import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.vaadin.spring.http.HttpService;
import org.vaadin.spring.security.annotation.EnableVaadinSharedSecurity;
import org.vaadin.spring.security.config.VaadinSharedSecurityConfiguration;
import org.vaadin.spring.security.shared.VaadinAuthenticationSuccessHandler;
import org.vaadin.spring.security.shared.VaadinUrlAuthenticationSuccessHandler;
import org.vaadin.spring.security.web.VaadinRedirectStrategy;

/**
 * Configure Spring Security.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, proxyTargetClass = true)
@EnableVaadinSharedSecurity
public class WebApplicationSecurityConfiguration extends WebSecurityConfigurerAdapter {

   @Autowired
   ManagerHome managerHome;

   @Override
   public void configure(AuthenticationManagerBuilder auth) throws Exception {

      DirectoryServiceConfiguration dsc = managerHome.getConfiguration(DirectoryServiceConfiguration.class);
      // FIXME ou=openthinclient is something that the user actually configures. It should not be hardcoded here!
      String ldapUrl = "ldap://localhost:" + dsc.getEmbeddedLdapPort() + "/ou=openthinclient," + dsc.getEmbeddedCustomRootPartitionName();

      // ou=dings,rootPartName
      // ou=openthinclient,dn=openthinclient,dn=org

      final LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> ldapAuthBuilder = auth.ldapAuthentication();

      ldapAuthBuilder.contextSource() //
            .url(ldapUrl) //
            .managerDn(dsc.getContextSecurityPrincipal()) //
            .managerPassword(dsc.getContextSecurityCredentials());

      ldapAuthBuilder.userDnPatterns("cn={0},ou=users")
      //        .groupSearchBase("ou=groups")
            .contextSource();
   }

   @Override
   protected void configure(HttpSecurity http) throws Exception {
      http.csrf().disable(); // Use Vaadin's built-in CSRF protection instead

      // FIXME is there a way to read the vaadin.servlet.urlMapping property and adjust the following configuration appropriately?

      // @formatter:off
      http.authorizeRequests()
              .anyRequest().permitAll();

      http.httpBasic().disable();
      http.formLogin().disable();
      http.logout()
              .logoutUrl("/ui/logout")
              .logoutSuccessUrl("/")
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
      return new VaadinUrlAuthenticationSuccessHandler(httpService, vaadinRedirectStrategy, "/");
   }
}
