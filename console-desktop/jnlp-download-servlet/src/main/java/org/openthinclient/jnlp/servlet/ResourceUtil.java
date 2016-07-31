package org.openthinclient.jnlp.servlet;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;

/**
 * A utility class for access to to web application resources.
 */
public class ResourceUtil {

    /**
     * These are the static resources prefixes that spring will use.
     */
    private static final String[] SPRING_RES_PREFIXES = {
            "/public/",
            "/static/",
            "/resources/",
            "/META-INF/resources/"
    };


    public static URL getResource(ServletContext context, String orig_path) throws MalformedURLException {

        if (orig_path.startsWith("/"))
            orig_path = orig_path.substring(1);

        URL resource = context.getResource(orig_path);

        // when running using spring boot, context.getResource will not return the actual resources on the classpath.
        if (resource == null) {

            // search all known prefixes for the given resource.
            for (String prefix : SPRING_RES_PREFIXES) {
                resource = ResourceUtil.class.getResource(prefix + orig_path);

                if (resource != null)
                    break;
            }
        }

        return resource;
    }
}
