package org.openthinclient.pkgmgr;

import org.junit.rules.ExternalResource;

import java.net.MalformedURLException;
import java.net.URL;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;

public class DebianTestRepositoryServer extends ExternalResource {


    private Undertow undertow;
    private URL url;
    private final PathHandler rootHandler;

    public DebianTestRepositoryServer() {
        rootHandler = Handlers.path();

        // initialize with the default test repository
        final String classpathPrefix = "test-repository/";

        setRepository(classpathPrefix);
    }

    /**
     * Specify the classpath prefix of the repository to use.
     *
     * @param classpathPrefix a classpath prefix within which the resources will be exposed
     */
    public void setRepository(String classpathPrefix) {
        rootHandler.clearPaths();
        rootHandler.addPrefixPath("/", Handlers.resource( //
                new ClassPathResourceManager(getClass().getClassLoader(), classpathPrefix) //
        ).setDirectoryListingEnabled(true));
    }

    public static void main(String[] args) throws Throwable {
        final DebianTestRepositoryServer server = new DebianTestRepositoryServer();
        server.before();

        System.out.println(server.getServerUrl());
    }

    /**
     * Cucumber cannot deal with Junit @Rule
     * @throws Throwable
     */
    public void startManually() throws Throwable {
        before();
    }
    /**
     * Cucumber cannot deal with Junit @Rule
     */    
    public void stopManually() {
      after();
    }    
    
    @Override
    protected void before() {
        final int port = 19090;
        this.url = createUrl(port);

        this.undertow = Undertow.builder() //
                .addHttpListener(port, null) //
                .setHandler(rootHandler)//
                .build();
        undertow.start();
    }

    @Override
    protected void after() {
        try {
            undertow.stop();
            undertow = null;
            url = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private URL createUrl(int port) {
        try {
            return new URL("http", "localhost", port, "");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to create server access URL", e);
        }
    }

    public URL getServerUrl() {
        if (url == null) {
            throw new IllegalStateException("not started");
        }
        return url;
    }

}
