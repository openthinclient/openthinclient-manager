package org.openthinclient.console.configuration;

import org.openthinclient.common.model.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContextRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(ContextRegistry.class);
  public static final ContextRegistry INSTANCE = new ContextRegistry();
  public static final String PROPERTY_CODEBASE = System.getProperty("ThinClientManager.server.Codebase");

  private final Map<Realm, ApplicationContext> contexts;

  public ContextRegistry() {
    this.contexts = new ConcurrentHashMap<>();
  }


  public ApplicationContext getContext(Realm realm) {

    final ApplicationContext existing = contexts.get(realm);
    if (existing != null)
      return existing;

    String homeServer = null;

    if (null != realm.getSchemaProviderName())
      homeServer = realm.getSchemaProviderName();
    else if (null != realm.getConnectionDescriptor().getHostname())
      homeServer = realm.getConnectionDescriptor().getHostname();
    else
    try {
      final javax.jnlp.BasicService basicService =
              (javax.jnlp.BasicService) javax.jnlp.ServiceManager.
                      lookup("javax.jnlp.BasicService");
      homeServer = basicService.getCodeBase().getHost();
    } catch (Exception e) {
      LOG.warn("Failed to determine the codebase using the javax.jnlp.BasicService", e);
      // fallback. Try to get the codebase URL from the codebase property
      final String codebaseProperty = PROPERTY_CODEBASE;
      if (codebaseProperty != null) {
        LOG.info("Using the system property '{}'. Value: '{}'", PROPERTY_CODEBASE, codebaseProperty);

        try {
          homeServer = new URL(codebaseProperty).getHost();
        } catch (MalformedURLException e1) {
          LOG.error("Invalid url specified: {}", codebaseProperty);
          homeServer = null;
        }

      }

      // last resort, mostly useful for the development time localhost:
      if (homeServer == null) {
        LOG.warn("Falling back to localhost as the codebase URL");
        homeServer = "localhost";
      }
    }

    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    final HashMap<String, Object> properties = new HashMap<String, Object>();
    properties.put("manager.server.name", homeServer);
    ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("dynamic", properties));
    ctx.register(HttpInvokerConfiguration.class);
    ctx.refresh();

    contexts.put(realm, ctx);

    return ctx;
  }
}
