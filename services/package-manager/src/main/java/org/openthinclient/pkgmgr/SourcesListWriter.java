package org.openthinclient.pkgmgr;

import com.google.common.base.Strings;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class SourcesListWriter {

  private final String NL = "\n";

  public void write(SourcesList sourcesList, OutputStream out) throws SourcesListWriterException {

    try {
      final OutputStreamWriter w = new OutputStreamWriter(out, "UTF-8");
      write(sourcesList, w);
      w.flush();
    } catch (IOException e) {
      throw new SourcesListWriterException("sources list writing failed", e);
    }

  }

  public void write(SourcesList sourcesList, Writer writer) throws SourcesListWriterException {

    sourcesList.getSources().forEach(source -> {

      try {
        if (!Strings.isNullOrEmpty(source.getDescription())) {
          final String[] lines = source.getDescription().split("\\r?\\n");
          if (lines.length > 0) {
            for (String line : lines) {
              writer.write("# ");
              writer.write(line);
              writer.write(NL);
            }
          }
        }

        if (!source.isEnabled()) {
          writer.write("#");
        }

        if (source.getType() == Source.Type.PACKAGE) {
          writer.write(SourcesListParser.DEB);
        } else {
          writer.write(SourcesListParser.DEB_SRC);
        }

        writer.write(" ");
        writer.write(source.getUrl().toExternalForm());
        writer.write(" ");
        writer.write(source.getDistribution());
        if (source.getComponents() != null && source.getComponents().size() > 0) {
          source.getComponents().forEach(comp -> {
            try {
              writer.write(" ");
              writer.write(comp);
            } catch (IOException e) {
              throw new SourcesListWriterException("sources list writing failed", e);
            }
          });
        }
        writer.write(NL);
      } catch (IOException e) {
        throw new SourcesListWriterException("sources list writing failed", e);
      }
    });

    try {
      writer.flush();
    } catch (IOException e) {
      throw new SourcesListWriterException("sources list writing failed", e);
    }
  }

  public static class SourcesListWriterException extends RuntimeException {
    public SourcesListWriterException() {
    }

    public SourcesListWriterException(String message) {
      super(message);
    }

    public SourcesListWriterException(String message, Throwable cause) {
      super(message, cause);
    }

    public SourcesListWriterException(Throwable cause) {
      super(cause);
    }
  }
}
