package org.openthinclient.web;

import com.vaadin.spring.annotation.EnableVaadin;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
import org.springframework.web.filter.OncePerRequestFilter;
import org.vaadin.spring.http.HttpService;
import org.vaadin.spring.security.annotation.EnableVaadinSharedSecurity;
import org.vaadin.spring.security.config.VaadinSharedSecurityConfiguration;
import org.vaadin.spring.security.shared.VaadinAuthenticationSuccessHandler;
import org.vaadin.spring.security.shared.VaadinRedirectLogoutHandler;
import org.vaadin.spring.security.shared.VaadinSessionClosingLogoutHandler;
import org.vaadin.spring.security.shared.VaadinUrlAuthenticationSuccessHandler;
import org.vaadin.spring.security.web.VaadinRedirectStrategy;

/**
 * Configure Spring Security.
 */
@Configuration
@EnableVaadin
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true, proxyTargetClass = true)
@EnableVaadinSharedSecurity
public class WebApplicationSecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Autowired
  private ManagerHome managerHome;

  @Value("${vaadin.servlet.urlMapping}")
  private String vaadinServletUrlMapping;

  @Autowired
  private VaadinRedirectStrategy redirectStrategy;


  @Override
  protected UserDetailsService userDetailsService() {
    return new LdapUserDetailsService(userSearch(), defaultLdapAuthoritiesPopulator());
  }

  @Override
  public void configure(AuthenticationManagerBuilder auth) throws Exception {

    DirectoryServiceConfiguration dsc = managerHome.getConfiguration(DirectoryServiceConfiguration.class);
    String ldapUrl = createLdapURL(dsc);

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

  @Bean
  public DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator() {
    DefaultLdapAuthoritiesPopulator ldapAuthoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource(), "cn=administrators,ou=RealmConfiguration") {
      @Override
      public Set<GrantedAuthority> getGroupMembershipRoles(String userDn, String username) {
        final Set<GrantedAuthority> roles = super.getGroupMembershipRoles(userDn, username);

        final Optional<GrantedAuthority> role = roles.stream().filter(r -> "ROLE_ADMINISTRATORS".equals(r.getAuthority())).findFirst();

        if (!role.isPresent())
          throw new BadCredentialsException("User is not allowed to login as an administrator");
        return roles;
      }
    };
    ldapAuthoritiesPopulator.setGroupRoleAttribute("cn");
    ldapAuthoritiesPopulator.setGroupSearchFilter("uniquemember={0}");
    ldapAuthoritiesPopulator.setSearchSubtree(true);
    return ldapAuthoritiesPopulator;
  }

  @Bean
  public LdapUserSearch userSearch() {
    return new FilterBasedLdapUserSearch("ou=users", "(cn={0})", contextSource());
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

  /**
   * Return the Ldap connection URL using parameters form configuration
   * @param dsc the DirectoryServiceConfiguration
   * @return the Ldap connction URL
   */
  private String createLdapURL(DirectoryServiceConfiguration dsc) {
    return "ldap://localhost:" + dsc.getEmbeddedLdapPort() + "/ou=" + dsc.getPrimaryOU() + "," + dsc.getEmbeddedCustomRootPartitionName();
  }

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
    redirectFilter.addUrlPatterns(vaadinServletUrlMapping + "first-start");

    redirectFilter.setFilter(new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
        response.sendRedirect(vaadinServletUrlMapping);
      }
    });
    return redirectFilter;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable();

    http.authorizeRequests()
        .antMatchers(vaadinServletUrlMapping + "login/**").anonymous()
        .antMatchers(vaadinServletUrlMapping + "UIDL/**").permitAll()
        .antMatchers(vaadinServletUrlMapping + "HEARTBEAT/**").permitAll()
        .anyRequest().authenticated();

    http.httpBasic().disable();
    http.formLogin().disable();

    http.logout()
        .addLogoutHandler(new VaadinSessionClosingLogoutHandler())
        .logoutUrl(vaadinServletUrlMapping + "logout")
        .logoutSuccessUrl(vaadinServletUrlMapping + "login?logout")
        .permitAll();

    http.exceptionHandling().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(vaadinServletUrlMapping + "login"));

    http.rememberMe().rememberMeServices(rememberMeServices()).key("openthinclient-manager");

    http.sessionManagement().sessionAuthenticationStrategy(sessionAuthenticationStrategy());
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    web.ignoring().antMatchers("/VAADIN/**")
                  .antMatchers("/actuator/health")
                  .antMatchers("/api/v1/**")
                  .antMatchers("/openthinclient/files/**")
                  .antMatchers("/ws/**")
    ;
  }

  @Override
  @Bean
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  @Bean
  public RememberMeServices rememberMeServices() {
    return new TokenBasedRememberMeServices("openthinclient-manager", userDetailsService());
  }

  @Bean
  public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    return new SessionFixationProtectionStrategy();
  }

  @Bean(name = VaadinSharedSecurityConfiguration.VAADIN_AUTHENTICATION_SUCCESS_HANDLER_BEAN)
  public VaadinAuthenticationSuccessHandler vaadinAuthenticationSuccessHandler(HttpService httpService, VaadinRedirectStrategy vaadinRedirectStrategy) {
    return new VaadinUrlAuthenticationSuccessHandler(httpService, vaadinRedirectStrategy, vaadinServletUrlMapping);
  }

  @Bean(name = VaadinSharedSecurityConfiguration.VAADIN_LOGOUT_HANDLER_BEAN)
  public VaadinRedirectLogoutHandler vaadinRedirectLogoutHandler(VaadinRedirectStrategy vaadinRedirectStrategy) {
    return new VaadinRedirectLogoutHandler(vaadinRedirectStrategy, vaadinServletUrlMapping + "logout");
  }

}

