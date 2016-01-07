package org.openthinclient.web;

import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.vaadin.spring.http.HttpService;
import org.vaadin.spring.security.annotation.EnableVaadinSharedSecurity;
import org.vaadin.spring.security.config.VaadinSharedSecurityConfiguration;
import org.vaadin.spring.security.web.VaadinRedirectStrategy;
import org.vaadin.spring.security.web.authentication.VaadinAuthenticationSuccessHandler;
import org.vaadin.spring.security.web.authentication.VaadinUrlAuthenticationSuccessHandler;

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
      String ldapUrl = "ldap://localhost:" + dsc.getEmbeddedLdapPort() + "/" + dsc.getEmbeddedCustomRootPartitionName();

      auth.ldapAuthentication().userDnPatterns("cn={0}").userSearchBase("ou=users")
      //        .groupSearchBase("ou=groups")
            .contextSource()
            //            .ldif("classpath:test-server.ldif")
            .url(ldapUrl);
   }

   @Override
   protected void configure(HttpSecurity http) throws Exception {
      http.csrf().disable(); // Use Vaadin's built-in CSRF protection instead

      // XXX keep in mind that this must be corrected using

      // @formatter:off
      http.authorizeRequests()
              .antMatchers("/ui/login/**").anonymous()
              .antMatchers("/vaadinServlet/UIDL/**").permitAll()
              .antMatchers("/vaadinServlet/HEARTBEAT/**").permitAll()
              .anyRequest().authenticated();
      http.httpBasic().disable();
      http.formLogin().disable();
      http.logout()
              .logoutUrl("/ui/logout")
              .logoutSuccessUrl("/ui/login?logout")
              .permitAll();
      http.exceptionHandling()
              .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));
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
