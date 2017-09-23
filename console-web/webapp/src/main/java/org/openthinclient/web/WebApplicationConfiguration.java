package org.openthinclient.web;

import com.vaadin.server.CustomizedSystemMessages;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.SystemMessagesInfo;
import com.vaadin.server.SystemMessagesProvider;

import org.openthinclient.api.logs.LogMvcConfiguration;
import org.openthinclient.api.rest.RestApiConfiguration;
import org.openthinclient.api.rest.ApplianceRestApiConfiguration;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.web.support.config.SystemReportingConfiguration;
import org.openthinclient.web.view.DashboardSections;
import org.openthinclient.web.view.dashboard.DashboardNotificationService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.vaadin.spring.i18n.MessageProvider;
import org.vaadin.spring.i18n.ResourceBundleMessageProvider;
import org.vaadin.spring.sidebar.annotation.EnableSideBar;

import java.util.Locale;

/**
 *
 */
@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class)
@EnableSideBar
@Import({
        WebApplicationSecurityConfiguration.class,
        VaadinCustomizationConfiguration.class,
        RestApiConfiguration.class,
        ApplianceRestApiConfiguration.class,
        LogMvcConfiguration.class,
        SystemReportingConfiguration.class
})
public class WebApplicationConfiguration {

    /**
     * Provide custom system messages to make sure the application is reloaded when the session
     * expires.
     *
     * @return SystemMessagesProvider
     */
    @Bean
    SystemMessagesProvider systemMessagesProvider() {
        return new SystemMessagesProvider() {

            /**
             * serialVersionUID
             */
            private static final long serialVersionUID = 2570216527087874367L;

            @Override
            public SystemMessages getSystemMessages(SystemMessagesInfo systemMessagesInfo) {
                CustomizedSystemMessages systemMessages = new CustomizedSystemMessages();
                systemMessages.setSessionExpiredNotificationEnabled(false);
                return systemMessages;
            }
        };
    }

    @Bean
    MessageProvider communicationMessages() {
        return new ResourceBundleMessageProvider("i18n/console-web-messages"); // Will use UTF-8 by default
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(Locale.GERMAN);
        return slr;
    }

    /**
     * Creates the DashboardSections meta bean. This bean is only required for the {@link
     * org.vaadin.spring.sidebar.SideBarUtils} to pickup the defined dashboard sections.
     */
    @Bean
    public DashboardSections dashboardSections() {
        return new DashboardSections();
    }

    @Bean
    public SchemaService schemaService(PackageManager packageManager, ApplicationService applicationService, SchemaProvider schemaProvider, RealmService realmService) {
        return new SchemaService(packageManager, applicationService, schemaProvider, realmService);
    }


    @Bean
    public DashboardNotificationService dashboardNotificationService() {
        return new DashboardNotificationService.Dummy();
    }
}
