package org.openthinclient.meta;

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Serves as a registry for discovered and loaded {@link PackageMetadata}.
 */
public class PackageMetadataManager {
  private static final Logger LOG = LoggerFactory.getLogger(PackageMetadataManager.class);

  private final Path baseDirectory;
  private final Path managerHomeRoot;
  private final PackageMetadataReader reader;

  public PackageMetadataManager(Path baseDirectory, final Path managerHomeRoot) {
    this.baseDirectory = baseDirectory;
    this.managerHomeRoot = managerHomeRoot;
    reader = new PackageMetadataReader();
  }

  public Stream<PackageMetadata> getMetadata() {
    final Stream<Path> files;
    try {
      files = Files.list(baseDirectory);
    } catch (IOException e) {
      LOG.error("Failed to list contents from the metadata directory", e);
      return Stream.empty();
    }
    return files //
            .filter(this::isPackageMetadataFile) //
            .map(this::parsePackageMetadataFile) //
            // filter all possible read failures (all null)
            .filter(Objects::nonNull);
  }

  private PackageMetadata parsePackageMetadataFile(Path p) {
    try {
      return reader.read(p);
    } catch (Exception e) {
      LOG.error("Failed to parse " + p.toString(), e);
      return null;
    }
  }

  /**
   * Reads and returns all available {@link Bookmark} definitions. This method will filter all
   * invalid {@link Bookmark}s by validating that the given {@link Bookmark#getPath() path}
   * actually exists.
   *
   * @return a {@link Stream} of {@link Bookmark}s for all valid {@link PackageMetadata package metadata files}
   */
  public Stream<Bookmark> getBookmarks() {
    return getMetadata().flatMap(m -> m.getBookmarks().stream())
            .filter(bookmark -> {
              if(Strings.isNullOrEmpty(bookmark.getPath())) {
                LOG.error("Ignoring bookmark entry without valid path");
                return false;
              }
              final Path path = Paths.get(bookmark.getPath());

              if (!Files.exists(managerHomeRoot.resolve(path))) {
                LOG.error("Ignoring bookmark for invalid path: " + bookmark.getPath());
                return false;
              }

              return true;
            });
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
