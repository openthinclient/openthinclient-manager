package org.openthinclient.manager.standalone.config.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URI;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {WebstartServletConfiguration.class, EmbeddedServletContainerAutoConfiguration.class})
public class WebstartServletConfigurationTest {

    @LocalServerPort
    int serverPort;

    @Test
    public void testDownloadJarFile() throws Exception {

        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        final ClientHttpRequest request = requestFactory.createRequest(new URI("http://localhost:" + serverPort + "/test.jar"), HttpMethod.GET);

        final ClientHttpResponse response = request.execute();


        assertEquals(HttpStatus.OK, response.getStatusCode());


    }

}