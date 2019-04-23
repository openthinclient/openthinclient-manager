package org.openthinclient.manager.standalone;

import org.junit.Test;

import java.net.URL;
import java.util.Enumeration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ManagerStandaloneServerApplicationTest {

    @Test
    public void testOnlyOneSingleApplicationPropertiesIsOnClasspath() throws Exception {

        final Enumeration<URL> resources = getClass().getClassLoader().getResources("application.properties");

        assertNotNull(resources);

        resources.nextElement();

        assertFalse("there seems to be more than only one __application.properties on the classpath", resources.hasMoreElements());


    }
}