//package org.openthinclient.manager.standalone.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
//
//@Configuration
//@EnableWebMvcSecurity
//public class SecurityConfig extends WebSecurityConfigurerAdapter {
//  @Override
//  protected void configure(HttpSecurity http) throws Exception {
//
//    // at the moment we're allowing all services to be called without any authentication.
//    // FIXME implement authentication for httpinvoker services
//    http
//            .authorizeRequests()
//            .antMatchers("/service/httpinvoker/**").permitAll();
//  }
//
//}