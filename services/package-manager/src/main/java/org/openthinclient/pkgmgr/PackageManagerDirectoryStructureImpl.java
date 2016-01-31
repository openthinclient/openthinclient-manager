package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.connect.PackageListDownloader;
import org.openthinclient.pkgmgr.db.Source;

import java.nio.file.Path;

public class PackageManagerDirectoryStructureImpl implements PackageManagerDirectoryStructure {

   private final PackageManagerConfiguration configuration;

   public PackageManagerDirectoryStructureImpl(PackageManagerConfiguration configuration) {this.configuration = configuration;}

   private String asChangelogDirectoryName(Source source) {
      // creating the initial changelog directory as a filename constructed using the host and the realtive path to the Packages.gz (without the Packages.gz itself)
      String changelogdir = source.getUrl().getHost() + "_" + source.getUrl().getFile().replace(PackageListDownloader.PACKAGES_GZ, "");
      if (changelogdir.endsWith("/"))
         changelogdir = changelogdir.substring(0, changelogdir.lastIndexOf("/"));
      changelogdir = changelogdir.replace('/', '_');
      changelogdir = changelogdir.replaceAll("\\.", "_");
      changelogdir = changelogdir.replaceAll("-", "_");
      changelogdir = changelogdir.replaceAll(":", "_COLON_");
      return changelogdir;
   }

   @Override
   public Path changelogFileLocation(Source source, org.openthinclient.util.dpkg.Package pkg) {

      final Path changelogRoot = configuration.getListsDir().toPath();

      return changelogRoot.resolve(asChangelogDirectoryName(source)).resolve(pkg.getName() + ".changelog");
   }
}
