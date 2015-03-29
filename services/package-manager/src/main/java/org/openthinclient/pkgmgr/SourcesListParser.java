package org.openthinclient.pkgmgr;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SourcesListParser {

  public static final String DEB_SRC = "deb-src";
  public static final String DEB = "deb";

  public SourcesList parse(Path file) {
    try {
      final List<String> lines = Files.readAllLines(file);

      return parse(lines);
    } catch (IOException e) {
      throw new SourcesListException(e);
    }

  }

  public SourcesList parse(List<String> lines) {

    final SourcesList sourcesList = new SourcesList();

    final List<String> commentLines = new ArrayList<>();

    lines.forEach(line -> {
      String trimmedLine = line.trim();

      List<String> prefixes = Arrays.asList("#" + DEB_SRC, "#" + DEB, DEB_SRC, DEB);

      if (prefixes.stream().anyMatch(trimmedLine::startsWith)) {
        boolean enabled = true;
        if (trimmedLine.startsWith("#")) {
          trimmedLine = trimmedLine.substring(1);
          enabled = false;
        }

        Source source = parseSource(trimmedLine);
        source.setEnabled(enabled);
        source.setDescription(commentLines.stream().collect(Collectors.joining("\n")));
        commentLines.clear();
        sourcesList.getSources().add(source);
      } else if (trimmedLine.startsWith("#")) {
        // comment line
        commentLines.add(trimmedLine.substring(1));
      } else if (!trimmedLine.isEmpty()) {
        // line with unknown content
        throw new SourcesListException("Illegal content in sources.list. Line'" + line +
                "'");
      }

    });

    return sourcesList;
  }

  protected Source parseSource(String trimmedLine) {
    final Source source = new Source();

    final String[] segments = trimmedLine.split("\\s");

    if (segments.length < 2) {
      throw new SourcesListException("incorrect line '" + trimmedLine +
              "'");
    }

    if (DEB.equals(segments[0])) {
      source.setType(Source.Type.PACKAGE);
    } else if (DEB_SRC.equals(segments[0])) {
      source.setType(Source.Type.PACKAGE_SOURCE);
    } else {
      throw new SourcesListException("Illegal type: " + segments[0]);
    }

    try {
      source.setUrl(new URL(segments[1]));
    } catch (MalformedURLException e) {
      throw new SourcesListException("Illegal url: '" + segments[1] + "'");
    }

    source.setDistribution(segments[2]);

    for (int i = 3; i < segments.length; i++) {
      source.getComponents().add(segments[i]);
    }

    return source;
  }

}
