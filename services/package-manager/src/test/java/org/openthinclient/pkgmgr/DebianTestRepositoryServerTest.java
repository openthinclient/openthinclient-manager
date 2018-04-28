package org.openthinclient.pkgmgr;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class DebianTestRepositoryServerTest {

  @Rule
  public DebianTestRepositoryServer repositoryServerUnderTest = new DebianTestRepositoryServer();

  @Test
  public void testChangeUrl() throws Exception {

    final URL url = new URL(repositoryServerUnderTest.getServerUrl(), "test.txt");
    repositoryServerUnderTest.setRepository("mock-server-test/01");

    try(final InputStream in = url.openStream()) {
      final String s = IOUtils.toString(in);
      assertEquals("Contents 01", s);
    }

    // changing the base classpath prefix
    repositoryServerUnderTest.setRepository("mock-server-test/02");

    try(final InputStream in = url.openStream()) {
      final String s = IOUtils.toString(in);
      assertEquals("Contents 02", s);
    }

  }
}