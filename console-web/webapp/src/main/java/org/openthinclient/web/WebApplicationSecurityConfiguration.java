package org.openthinclient.web;

import static org.openthinclient.web.WebUtil.getServletMappingRoot;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.security.VaadinTokenBasedRememberMeServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.web.filter.OncePerRequestFilter;
import org.vaadin.spring.security.annotation.EnableVaadinManagedSecurity;
import org.vaadin.spring.security.config.AuthenticationManagerConfigurer;

/**
 * Configure Spring Security.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, proxyTargetClass = true)
@EnableVaadinManagedSecurity
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
     redirectFilter.addUrlPatterns(getServletMappingRoot(vaadinServletUrlMapping) + "first-start");

     redirectFilter.setFilter(new OncePerRequestFilter() {
       @Override
       protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
         response.sendRedirect(getServletMappingRoot(vaadinServletUrlMapping));
       }
     });
     return redirectFilter;
   }

  @Configuration
  class AuthenticationConfiguration implements AuthenticationManagerConfigurer {


    @Override
   public void configure(AuthenticationManagerBuilder auth) throws Exception {

      DirectoryServiceConfiguration dsc = managerHome.getConfiguration(DirectoryServiceConfiguration.class);
      String ldapUrl = createLdapURL(dsc);

      LOG.info("Configuring authentication for LDAP: {}", ldapUrl);

      final LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> ldapAuthBuilder = auth.ldapAuthentication();

      ldapAuthBuilder.contextSource() //
            .url(ldapUrl) //
            .managerDn(dsc.getContextSecurityPrincipal()) //
            .managerPassword(dsc.getContextSecurityCredentials());

       ldapAuthBuilder
               .userDnPatterns("cn={0},ou=users")
               .ldapAuthoritiesPopulator(defaultLdapAuthoritiesPopulator())
               .contextSource();
   }

  }
    @Bean
    public DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator() {
        DefaultLdapAuthoritiesPopulator ldapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource(), "cn=administrators,ou=RealmConfiguration");
        ldapAuthoritiesPopulator.setGroupRoleAttribute("cn");
        ldapAuthoritiesPopulator.setGroupSearchFilter("uniquemember={0}");
        ldapAuthoritiesPopulator.setSearchSubtree(true);
        return ldapAuthoritiesPopulator;
    }

  /**
   * Return the Ldap connection URL using parameters form configuration
   * @param dsc the DirectoryServiceConfiguration
   * @return the Ldap connction URL
   */
  private String createLdapURL(DirectoryServiceConfiguration dsc) {
    return "ldap://localhost:" + dsc.getEmbeddedLdapPort() + "/ou=" + dsc.getPrimaryOU() + "," + dsc.getEmbeddedCustomRootPartitionName();
  }

  @Bean
  public BaseLdapPathContextSource contextSource() {
    DirectoryServiceConfiguration dsc = managerHome.getConfiguration(DirectoryServiceConfiguration.class);
    String ldapUrl = createLdapURL(dsc);
    DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldapUrl);
    contextSource.setUserDn(dsc.getContextSecurityPrincipal());
    contextSource.setPassword(dsc.getContextSecurityCredentials());
    return contextSource;
  }

  @Bean
  public LdapUserSearch userSearch() {
    return new FilterBasedLdapUserSearch("ou=users", "(cn={0})", contextSource());
  }

    @Override
    protected UserDetailsService userDetailsService() {
      return new LdapUserDetailsService(userSearch(), defaultLdapAuthoritiesPopulator());
    }

    @Bean
    public RememberMeServices rememberMeServices() {
      // TODO Is there some way of exposing the RememberMeServices instance that the remember me configurer creates by default?
      VaadinTokenBasedRememberMeServices services = new VaadinTokenBasedRememberMeServices("openthinclient-manager", userDetailsService());
      services.setAlwaysRemember(false);
      return services;
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
              .deleteCookies("JSESSIONID")
              .permitAll();
      http.rememberMe().rememberMeServices(rememberMeServices()).key("openthinclient-manager");
      // @formatter:on
   }

   @Override
   public void configure(WebSecurity web) throws Exception {
      web.ignoring().antMatchers("/VAADIN/**");
   }

}
