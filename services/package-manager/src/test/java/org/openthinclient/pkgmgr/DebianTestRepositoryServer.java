package org.openthinclient.pkgmgr;

import org.junit.rules.ExternalResource;

import java.net.MalformedURLException;
import java.net.URL;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.resource.ClassPathResourceManager;

public class DebianTestRepositoryServer extends ExternalResource {


    private Undertow undertow;
    private URL url;

    public static void main(String[] args) throws Throwable {
        final DebianTestRepositoryServer server = new DebianTestRepositoryServer();
        server.before();

        System.out.println(server.getServerUrl());
    }

    @Override
    protected void before() throws Throwable {
        final int port = 19090;
        this.url = createUrl(port);
        this.undertow = Undertow.builder() //
                .addHttpListener(port, null) //
                .setHandler(Handlers.resource( //
                        new ClassPathResourceManager(getClass().getClassLoader(), "test-repository/") //
                ).setDirectoryListingEnabled(true))//
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
