package org.openthinclient.api.rest;

import com.google.common.base.Predicate;

import org.openthinclient.api.rest.impl.ProfileRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;
import static springfox.documentation.builders.RequestHandlerSelectors.any;

@Configuration
// scan for components in the package which contains ModelRepository
@ComponentScan(basePackageClasses = ProfileRepository.class)
@EnableSwagger2
public class RestApiConfiguration {

    @Bean
    public Docket swaggerSpringMvcPlugin() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("business-api")
                .select()
                //Ignores controllers annotated with @CustomIgnore
                .apis(any()) //Selection by RequestHandler
                .paths(paths()) // and by paths
                .build()
                .apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        return new ApiInfo("Manager API", "Management API for ThinClients", "1.0", "", new Contact("openthinclient.org", "http://www.openthinclient.org", "info@openthinclient.org"), null, null);
    }

    //Here is an example where we select any api that matches one of these paths
    private Predicate<String> paths() {
        return or(
                regex("/api/.*"));
    }

}
