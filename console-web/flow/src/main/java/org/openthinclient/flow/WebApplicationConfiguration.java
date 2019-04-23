package org.openthinclient.flow;

import com.vaadin.flow.spring.annotation.EnableVaadin;
import org.openthinclient.api.logs.LogMvcConfiguration;
import org.openthinclient.api.rest.ApplianceRestApiConfiguration;
import org.openthinclient.api.rest.RestApiConfiguration;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.flow.misc.SchemaService;
import org.openthinclient.pkgmgr.PackageManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;


import java.util.Locale;

/**
 *
 */
//@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class)
@EnableVaadin(value = "org.openthinclient.flow")
@Configuration
@Import({
        SecurityConfiguration.class,
        RestApiConfiguration.class,
        ApplianceRestApiConfiguration.class,
        LogMvcConfiguration.class
})
public class WebApplicationConfiguration {

    /**
     * Provide custom system messages to make sure the application is reloaded when the session
     * expires.
     *
     * @return SystemMessagesProvider
     */
//    @Bean
//    SystemMessagesProvider systemMessagesProvider() {
//        return new SystemMessagesProvider() {
//
//            /**
//             * serialVersionUID
//             */
//            private static final long serialVersionUID = 2570216527087874367L;
//
//            @Override
//            public SystemMessages getSystemMessages(SystemMessagesInfo systemMessagesInfo) {
//                CustomizedSystemMessages systemMessages = new CustomizedSystemMessages();
//                systemMessages.setSessionExpiredNotificationEnabled(false);
//                return systemMessages;
//            }
//        };
//    }

//    @Bean
//    MessageProvider communicationMessages() {
//        return new ResourceBundleMessageProvider("i18n/console-web-messages"); // Will use UTF-8 by default
//    }

//    @Bean
//    public ManagerHome managerHome() {
//        ManagerHomeFactory managerHomeFactory = new ManagerHomeFactory();
//        return new DefaultManagerHome(managerHomeFactory.getManagerHomeDirectory());
//    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(Locale.GERMAN);
        return slr;
    }

    @Bean
    public SchemaService schemaService(PackageManager packageManager, ApplicationService applicationService, SchemaProvider schemaProvider, RealmService realmService) {
        return new SchemaService(packageManager, applicationService, schemaProvider, realmService);
    }

}
