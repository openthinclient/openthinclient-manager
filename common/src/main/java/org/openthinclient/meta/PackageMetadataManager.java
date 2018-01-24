package org.openthinclient.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Serves as a registry for discovered and loaded {@link PackageMetadata}.
 */
public class PackageMetadataManager {
  private static final Logger LOG = LoggerFactory.getLogger(PackageMetadataManager.class);

  private final Path baseDirectory;

  public PackageMetadataManager(Path baseDirectory) {
    this.baseDirectory = baseDirectory;
  }

  public Stream<PackageMetadata> getMetadata() {
    final PackageMetadataReader reader = new PackageMetadataReader();
    final Stream<Path> files;
    try {
      files = Files.list(baseDirectory);
    } catch (IOException e) {
      LOG.error("Failed to list contents from the metadata directory", e);
      return Stream.empty();
    }
    return files
            .filter(this::isPackageMetadataFile)
            .map(p -> {
              try {
                return reader.read(p);
              } catch (Exception e) {
                LOG.error("Failed to parse " + p.toString(), e);
                return null;
              }
            })
            // filter all possible read failures (all null)
            .filter(Objects::nonNull);
  }

  /**
   * Checks whether or not the given file is a possible {@link PackageMetadata metadata file}.
   * Every {@link Files#isRegularFile(Path, LinkOption...) reglar file} with the file extension
   * <code>.meta.xml</code> is a validate candidate.
   */
  private boolean isPackageMetadataFile(Path p) {
    return Files.isRegularFile(p) && p.getFileName().toString().endsWith(".meta.xml");
  }
}
