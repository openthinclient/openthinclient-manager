package org.openthinclient.runtime.web.comptest;

import com.vaadin.spring.annotation.EnableVaadin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SpringBootWebSecurityConfiguration;

@SpringBootApplication(exclude = {
        LiquibaseAutoConfiguration.class,
        SpringBootWebSecurityConfiguration.class,
        SecurityAutoConfiguration.class
})
@EnableVaadin
public class ComponentTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(ComponentTestApplication.class, args);
    }
}
