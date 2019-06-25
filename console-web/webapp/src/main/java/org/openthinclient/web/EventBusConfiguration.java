package org.openthinclient.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.support.ApplicationContextEventBroker;


/**
 * Forward Spring ApplicationEvents to Vaadin4Spring EventBus
 */
@Configuration
public class EventBusConfiguration {
    @Autowired
    EventBus.ApplicationEventBus applicationEventBus;

    @Bean
    ApplicationContextEventBroker applicationContextEventBroker() {
        return new ApplicationContextEventBroker(applicationEventBus);
    }
}
