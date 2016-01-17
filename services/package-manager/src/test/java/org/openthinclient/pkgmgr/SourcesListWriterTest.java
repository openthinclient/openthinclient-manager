package org.openthinclient.pkgmgr;

import org.junit.Test;
import org.openthinclient.pkgmgr.db.Source;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import static junit.framework.Assert.assertEquals;

public class SourcesListWriterTest {
  @Test
  public void testWriteSingleSourceNoComment() throws Exception {

    final SourcesList sl = new SourcesList();
    sl.getSources().add(createEnabledSource("http://some.host/url"));

    writeAndCompare("deb http://some.host/url ./\n", sl);

  }

  @Test
  public void testWriteSingleDisabledSourceNoComment() throws Exception {
    final SourcesList sl = new SourcesList();
    sl.getSources().add(createDisabledSource("http://some.host/url"));

    writeAndCompare("#deb http://some.host/url ./\n", sl);

  }

  @Test
  public void testWriteMultipleSourcesNoComment() throws Exception {
    final SourcesList sl = new SourcesList();

    sl.getSources().add(createEnabledSource("http://some.host/url"));
    sl.getSources().add(createDisabledSource("http://some.host/url"));
    sl.getSources().add(createEnabledSource("http://another.host/path"));

    writeAndCompare("deb http://some.host/url ./\n" + //
            "#deb http://some.host/url ./\n" + //
            "deb http://another.host/path ./\n", sl);
  }

  @Test
  public void testWriteSingleSourceWithComment() throws Exception {
    final SourcesList sl = new SourcesList();
    sl.getSources().add(createEnabledSource("http://some.host/url", "This is a comment\nspanning multiple lines"));

    writeAndCompare("# This is a comment\n" +
            "# spanning multiple lines\n" +
            "deb http://some.host/url ./\n", sl);

  }

  private void writeAndCompare(String expected, SourcesList sl) {
    final SourcesListWriter w = new SourcesListWriter();
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    w.write(sl, baos);

    assertEquals(expected, new String(baos.toByteArray()));
  }

  private Source createEnabledSource(String url) throws MalformedURLException {
    return createEnabledSource(url, null);
  }

  private Source createEnabledSource(String url, String description) throws MalformedURLException {
    final Source source = new Source();
    source.setDescription(description);
    source.setEnabled(true);
    source.setUrl(new URL(url));
    return source;
  }

  private Source createDisabledSource(String url) throws MalformedURLException {
    return createDisabledSource(url, null);
  }

  private Source createDisabledSource(String url, String description) throws MalformedURLException {
    final Source source = new Source();
    source.setDescription(description);
    source.setEnabled(false);
    source.setUrl(new URL(url));
    return source;
  }
}
