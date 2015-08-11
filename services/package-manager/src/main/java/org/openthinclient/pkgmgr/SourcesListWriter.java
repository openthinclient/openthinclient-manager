package org.openthinclient.pkgmgr;

import com.google.common.base.Strings;

import java.io.OutputStream;
import java.io.PrintWriter;

public class SourcesListWriter {

  public void write(SourcesList sourcesList, OutputStream out) {

    final PrintWriter printStream = new PrintWriter(out);

    sourcesList.getSources().forEach(source -> {

      if (!Strings.isNullOrEmpty(source.getDescription())) {
        final String[] lines = source.getDescription().split("\\r?\\n");
        if (lines.length > 0) {
          for (String line : lines) {
            printStream.print("# ");
            printStream.print(line);
            printStream.println();
          }
        }
      }

      if (!source.isEnabled()) {
        printStream.print("#");
      }

      if (source.getType() == Source.Type.PACKAGE) {
        printStream.print(SourcesListParser.DEB);
      } else {
        printStream.print(SourcesListParser.DEB_SRC);
      }

      printStream.print(" ");
      printStream.print(source.getUrl().toExternalForm());
      printStream.print(" ");
      printStream.print(source.getDistribution());
      if (source.getComponents() != null && source.getComponents().size() > 0) {
        source.getComponents().forEach(comp -> {
          printStream.print(" ");
          printStream.print(comp);
        });
      }
    });

    printStream.flush();
  }
}
