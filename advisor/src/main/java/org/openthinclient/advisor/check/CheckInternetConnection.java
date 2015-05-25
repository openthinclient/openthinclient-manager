package org.openthinclient.advisor.check;

import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.http.impl.HttpConnectionTester;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Die Klasse cInetConnection pr??ft, ob eine funktionierende Internetverbindung vorhanden ist.
 * @author Benedikt Diehl
 */
public class CheckInternetConnection {

    private NetworkConfiguration.ProxyConfiguration proxyConfiguration;

    public NetworkConfiguration.ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }

    public void setProxyConfiguration(NetworkConfiguration.ProxyConfiguration proxyConfiguration) {
        this.proxyConfiguration = proxyConfiguration;
    }

    /**
     * Verify the connectivity by contacting some well known sites.
     */
    public boolean checkConnectivity() {

        final HttpConnectionTester connectionTester = new HttpConnectionTester(proxyConfiguration);


        final List<HttpConnectionTester.Result> results = Stream.of(
                "http://www.openthinclient.org",
                "http://www.google.com",
                "http://www.levigo.de"
        )
                .map(URI::create)
                .map(HttpConnectionTester.Request::new)
                .map(connectionTester::verifyConnectivity)
                .collect(Collectors.toList());

        return results.stream().allMatch(HttpConnectionTester.Result::isSuccess);

    }

}
