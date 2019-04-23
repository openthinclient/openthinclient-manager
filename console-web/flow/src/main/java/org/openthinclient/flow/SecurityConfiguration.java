package org.openthinclient.flow;

import org.openthinclient.flow.misc.CustomRequestCache;
import org.openthinclient.flow.misc.SecurityUtils;
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
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import static org.openthinclient.web.WebUtil.getServletMappingRoot;

/**
 * Configures spring security, doing the following:
 * <li>Bypass security checks for static resources,</li>
 * <li>Restrict access to the application, allowing only logged in users,</li>
 * <li>Set up the login form</li>

 */
@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	private static final String LOGIN_PROCESSING_URL = "login";
	private static final String LOGIN_FAILURE_URL = "accessDenied";
	private static final String LOGIN_URL = "login";
	private static final String LOGOUT_SUCCESS_URL = "login";

	@Autowired
	private ManagerHome managerHome;

	@Value("${vaadin.servlet.urlMapping}")
	public String vaadinServletUrlMapping;


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
//		redirectFilter.addUrlPatterns("/");
		redirectFilter.addUrlPatterns(getServletMappingRoot(vaadinServletUrlMapping) + "first-start");

		redirectFilter.setFilter(new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException {
					response.sendRedirect(getServletMappingRoot(vaadinServletUrlMapping));
			}
		});
		return redirectFilter;
	}

	/**
	 * Require login to access internal pages and configure login form.
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// Not using Spring CSRF here to be able to use plain HTML for the login page
		http.csrf().disable()

				// Register our CustomRequestCache, that saves unauthorized access attempts, so
				// the user is redirected after login.
				.requestCache().requestCache(new CustomRequestCache())

				// Restrict access to our application.
				.and().authorizeRequests()

				// Allow all flow internal requests.
				.requestMatchers(SecurityUtils::isFrameworkInternalRequest).permitAll()

				// Allow all requests by logged in users.
				.anyRequest().authenticated()

				// Configure the login page.
				.and().formLogin()
				      .loginPage(getServletMappingRoot(vaadinServletUrlMapping) + LOGIN_URL).permitAll()
				      .loginProcessingUrl(getServletMappingRoot(vaadinServletUrlMapping) + LOGIN_PROCESSING_URL)
				      .failureUrl(getServletMappingRoot(vaadinServletUrlMapping) + LOGIN_FAILURE_URL)


				// Configure logout
				.and().logout().logoutSuccessUrl(getServletMappingRoot(vaadinServletUrlMapping) + LOGOUT_SUCCESS_URL);

		// TODO: use this for remember-me
		// http.rememberMe().rememberMeServices(rememberMeServices()).key("openthinclient-manager");
	}


	@Bean
	public RememberMeServices rememberMeServices() {
		return new TokenBasedRememberMeServices("openthinclient-manager", userDetailsService());
	}

	/**
	 * Allows access to static resources, bypassing Spring security.
	 */
	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers(
				// Vaadin Flow static resources
				"/VAADIN/**",

				// the standard favicon URI
				"/favicon.ico",

				// the robots exclusion standard
				"/robots.txt",

				// web application manifest
				"/manifest.webmanifest",
				"/sw.js",
				"/offline-page.html",

				// icons and images
				"/icons/**",
				"/images/**",

				// (development mode) static resources
				"/frontend/**",

				// (development mode) webjars
				"/webjars/**",

				// (development mode) H2 debugging console
				"/h2-console/**",

				// (production mode) static resources
				"/frontend-es5/**", "/frontend-es6/**");
	}

	@Override
  @Bean
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

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

}
