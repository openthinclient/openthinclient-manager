package org.openthinclient.pkgmgr;

import org.junit.Test;
import org.openthinclient.pkgmgr.db.Source;

import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class SourcesListParserTest {

  @Test
  public void testParseSingleLine() throws Exception {

    final SourcesList sourcesList = doParse("deb http://packages.openthinclient.org/openthinclient/v2/manager-rolling ./");

    assertEquals(1, sourcesList.getSources().size());

    assertSource(sourcesList.getSources().get(0), //
            true, // enabled
            "", // description
            new URL("http://packages.openthinclient.org/openthinclient/v2/manager-rolling") // url
            );

  }

  @Test
  public void testParseSingleLineWithComponents() throws Exception {

    final SourcesList sourcesList = doParse("deb http://packages.openthinclient.org/openthinclient/v2/manager-rolling ./ comp1 comp2 comp3");

    assertEquals(1, sourcesList.getSources().size());

    assertSource(sourcesList.getSources().get(0), //
            true, // enabled
            "", // description
            new URL("http://packages.openthinclient.org/openthinclient/v2/manager-rolling") // url
            );

  }

  @Test
  public void testParseSingleDisabledLineWithComponents() throws Exception {

    final SourcesList sourcesList = doParse("#deb http://packages.openthinclient.org/openthinclient/v2/manager-rolling ./ comp1 comp2 comp3");

    assertEquals(1, sourcesList.getSources().size());

    assertSource(sourcesList.getSources().get(0), //
            false, // enabled
            "", // description
            new URL("http://packages.openthinclient.org/openthinclient/v2/manager-rolling") // url
            );

  }

  @Test
  public void testParseSingleSourceWithDescription() throws Exception {

    final String expectedDescription = " This is a multi line comment\n" + //
            " describing the repository\n" + //
            "\n" + //
            " Even with empty lines";

    final SourcesList sourcesList = doParse("# This is a multi line comment", //
            "# describing the repository", //
            "#", //
            "# Even with empty lines", //
            "#deb http://packages.openthinclient.org/openthinclient/v2/manager-rolling ./");

    assertEquals(1, sourcesList.getSources().size());

    assertSource(sourcesList.getSources().get(0), //
            false, // enabled
            expectedDescription, // description
            new URL("http://packages.openthinclient.org/openthinclient/v2/manager-rolling") // url
            );

  }

  private void assertSource(Source line, boolean enabled, String description, URL url) {
    assertEquals(enabled, line.isEnabled());
    assertEquals(description, line.getDescription());
    assertEquals(url, line.getUrl());
  }

  private SourcesList doParse(String... lines) {
    final SourcesListParser parser = new SourcesListParser();

    return parser.parse(Arrays.asList(lines));
  }
}
