package org.openthinclient.web;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.vaadin.server.CustomizedSystemMessages;
import com.vaadin.server.SystemMessages;
import com.vaadin.server.SystemMessagesInfo;
import com.vaadin.server.SystemMessagesProvider;
import com.vaadin.spring.annotation.UIScope;
import org.openthinclient.api.logs.LogMvcConfiguration;
import org.openthinclient.api.rest.ApplianceRestApiConfiguration;
import org.openthinclient.api.rest.RestApiConfiguration;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.sidebar.OTCSideBarUtils;
import org.openthinclient.web.support.config.SystemReportingConfiguration;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.vaadin.spring.i18n.I18N;
import org.vaadin.spring.i18n.MessageProvider;
import org.vaadin.spring.i18n.ResourceBundleMessageProvider;
import org.vaadin.spring.sidebar.annotation.EnableSideBar;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
@EnableSideBar
@Import({
        WebApplicationSecurityConfiguration.class,
        VaadinCustomizationConfiguration.class,
        RestApiConfiguration.class,
        ApplianceRestApiConfiguration.class,
        LogMvcConfiguration.class,
        SystemReportingConfiguration.class
})
@EnableCaching
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

    @Bean(name = "settingsSideBar")
    @UIScope
    OTCSideBar settingsSideBar(OTCSideBarUtils utils) {
        return new OTCSideBar(ManagerSideBarSections.SERVER_MANAGEMENT, utils);
    }

    @Bean(name = "deviceSideBar")
    @UIScope
    OTCSideBar deviceSideBar(OTCSideBarUtils utils) {
        return new OTCSideBar(ManagerSideBarSections.DEVICE_MANAGEMENT, utils);
    }

    @Bean
    @UIScope
    OTCSideBarUtils sideBarUtils(ApplicationContext applicationContext, I18N i18n) {
        return new OTCSideBarUtils(applicationContext, i18n);
    }


    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(Locale.GERMAN);
        return slr;
    }

    /**
     * Creates the ManagerSideBarSections meta bean. This bean is only required for the {@link
     * org.vaadin.spring.sidebar.SideBarUtils} to pickup the defined dashboard sections.
     */
    @Bean
    public ManagerSideBarSections dashboardSections() {
        return new ManagerSideBarSections();
    }

    @Bean
    public SchemaService schemaService(PackageManager packageManager, ApplicationService applicationService, SchemaProvider schemaProvider, RealmService realmService) {
        return new SchemaService(packageManager, applicationService, schemaProvider, realmService);
    }


    @Bean
    public DashboardNotificationService dashboardNotificationService() {
        return new DashboardNotificationService.Dummy();
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("clientMetaData");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    Caffeine<Object,Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(600)
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .weakKeys()
            .recordStats();
    }
}
