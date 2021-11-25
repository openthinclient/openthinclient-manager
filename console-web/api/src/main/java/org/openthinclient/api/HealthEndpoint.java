package org.openthinclient.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple endpoint to check whether the server is up.
 *
 * This has been previously provided by the spring-boot-starter-actuator (which
 * is unmaintained and incompatible with newer springfox versions).
 */
@RestController
public class HealthEndpoint {

    @GetMapping("/actuator/health")
    public String actuatorHealth() {
        return "";
    }

}
