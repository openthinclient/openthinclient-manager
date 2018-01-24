package org.openthinclient.meta;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

public class PackageMetadataReader {

  private final JAXBContext context;

  public PackageMetadataReader() {
    try {
      context = JAXBContext.newInstance(PackageMetadata.class);
    } catch (JAXBException e) {
      throw new RuntimeException("JAXB initialization failed", e);
    }
  }

  public PackageMetadata read(Path file) {
    try (final InputStream in = Files.newInputStream(file)) {
      return read(in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public PackageMetadata read(InputStream in) {
    return read(new StreamSource(in));
  }

  public PackageMetadata read(Source source) {

    try {
      final JAXBElement<PackageMetadata> element = context.createUnmarshaller().unmarshal(source, PackageMetadata.class);
      return element.getValue();
    } catch (JAXBException e) {
      throw new RuntimeException("Package Metadata parsing failed", e);
    }

  }

}
