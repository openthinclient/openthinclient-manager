/*******************************************************************************
 * openthinclient.org ThinClient suite <p/> Copyright (C) 2004, 2007 levigo holding GmbH. All Rights
 * Reserved. <p/> <p/> This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. <p/> This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details. <p/> You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.pkgmgr;

import org.apache.commons.lang3.StringUtils;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.pkgmgr.connect.PackageListDownloader;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.progress.ProgressReceiver;
import org.openthinclient.progress.ProgressTask;
import org.openthinclient.util.dpkg.LocalPackageList;
import org.openthinclient.util.dpkg.PackagesListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UpdateDatabase implements ProgressTask<PackageListUpdateReport> {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateDatabase.class);
    private final PackageManagerConfiguration configuration;
    private final SourcesList sourcesList;
    private final PackageManagerDatabase db;
    private final PackageManagerDirectoryStructure directoryStructure;
    private final DownloadManager downloadManager;
    

    public UpdateDatabase(PackageManagerConfiguration configuration, SourcesList sourcesList, PackageManagerDatabase db, DownloadManager downloadManager) {
        this.configuration = configuration;
        this.sourcesList = sourcesList;
        this.db = db;
        this.directoryStructure = new PackageManagerDirectoryStructureImpl(configuration);
        this.downloadManager = downloadManager;
    }

   private static URL createPackageChangeLogURL(Package pkg) {
        try {
            URL serverPath = pkg.getSource().getUrl();
            if (!serverPath.toExternalForm().endsWith("/")) {
                serverPath = new URL(serverPath.toExternalForm() + "/");
            }

            return new URL(serverPath, pkg.getName() + ".changelog");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to access changelog due to illegal url", e);
        }
    }

    private boolean downloadChangelogFile(NetworkConfiguration.ProxyConfiguration proxyConfiguration, Source source, Package pkg,
                                          PackageManagerTaskSummary taskSummary) throws PackageManagerException {
        try {
            final Path changelogFile = directoryStructure.changelogFileLocation(source, pkg);
            Files.createDirectories(changelogFile.getParent());
            downloadManager.downloadTo(createPackageChangeLogURL(pkg), changelogFile.toFile());
            return true;
        } catch (final Exception e) {
            if (null != taskSummary) {
                taskSummary.addWarning(e.toString());
            }
            LOG.warn("Changelog download failed for package " + pkg.getName());
            return false;
        }
    }

    private void processPackage(Source source, Package updatedPkg, PackageListUpdateReport report) {

        final Package existing = db.getPackageRepository().getBySourceAndNameAndVersion(source, updatedPkg.getName(), updatedPkg.getVersion());

        if (existing != null) {
            if (existing.equals(updatedPkg)) {           
               LOG.info("Skipping already existing and equal {}", updatedPkg.toStringWithNameAndVersion());
               report.incSkipped();
            } else {
               LOG.info("Adding a new version: {}", existing.toStringWithNameAndVersion());
               // do a changelog update
               updatedPkg.setChangeLog(extractChangelogEntries(source, updatedPkg));
               db.getPackageRepository().save(updatedPkg);
               report.incUpdated();
            }
        } else {
            // get changelog for new package
            updatedPkg.setChangeLog(extractChangelogEntries(source, updatedPkg));

            LOG.info("Adding new package {}", updatedPkg.toStringWithNameAndVersion());
            db.getPackageRepository().save(updatedPkg);
            report.incAdded();
        }
        
    }

    /**
     * Debian changelog format: <a href="https://www.debian.org/doc/debian-policy/ch-source.html">Changelog Format Specification</a>
     * @param source the source
     * @param pkg the package
     * @return Changelog entries as String
     */
   private String extractChangelogEntries(Source source, Package pkg) {
      
      LOG.info("Extract the Changelog for {}", pkg.toStringWithNameAndVersion());
      
      PackageManagerTaskSummary taskSummary = new PackageManagerTaskSummary();
      downloadChangelogFile(configuration.getProxyConfiguration(), source, pkg, taskSummary);
      LOG.trace("taskSummary for downloadChangelogFile: {}" , taskSummary.getWarnings());

      // Regarding to debian-policy: the package-version is set in brackets, i.e. package: zonk (2.0-1) 
      String nameAndVersion = pkg.getName() + " (" + pkg.getDisplayVersion() + ")";
      List<String> lines = parseChangelogFile(source, pkg);
      StringBuilder sb = new StringBuilder();
      boolean addLines = false;
      for (String line : lines) {
         if (line.toLowerCase().contains(nameAndVersion.toLowerCase())) {
            addLines = true;
         }
         if (addLines && StringUtils.isNotBlank(line)) {
            sb.append(line).append("\n");
         }
      }
      return sb.toString();
   }

   /**
    * Parse the changelog file
    * @param source the package source
    * @param pkg the package
    * @return a list with parsed lines
    */
   private List<String> parseChangelogFile(Source source, Package pkg) {
      try {
         Path changelogFile = directoryStructure.changelogFileLocation(source, pkg);
         LOG.trace("changelogFile: {}", changelogFile);
         return Files.lines(changelogFile).collect(Collectors.toList());
      } catch (IOException e) {
         LOG.error("Cannot read changelogFile for package " + pkg.toStringWithNameAndVersion());
         return Collections.emptyList();
      }
   }

   private Stream<Package> parsePackagesList(LocalPackageList localPackageList) {
        LOG.info("Processing packages for {}", localPackageList.getSource().getUrl());

        try {
            return new PackagesListParser()
                    .parse(Files.newInputStream(localPackageList.getPackagesFile().toPath()))
                    .stream()
                    .map(p -> {
                        p.setSource(localPackageList.getSource());
                        return p;
                    });
        } catch (IOException e) {
            LOG.error("Failed to parse packages list for " + localPackageList.getSource().getUrl(), e);
            return Stream.empty();
        }
    }

    @Override
    public ProgressTaskDescription getDescription(Locale locale) {
        // FIXME
        return null;
    }

    @Override
    public PackageListUpdateReport execute(ProgressReceiver progressReceiver) {

        final PackageListDownloader packageListDownloader = new PackageListDownloader(configuration, downloadManager);

        final PackageListUpdateReport report = new PackageListUpdateReport();

        for (Source source : sourcesList.getSources()) {

            if (!source.isEnabled()) {
               LOG.info("Disabled source {} skipped.", source);
               continue;
            }

            updateProgress(progressReceiver, source);

            final LocalPackageList localPackageList = packageListDownloader.download(source, progressReceiver);
            List<Package> parsePackagesList = parsePackagesList(localPackageList).collect(Collectors.toList());
            parsePackagesList.forEach((pkg) -> processPackage(source, pkg, report));
            
            // get (already updated) package-list and remove outdated packages, disable installed outdated packages
            List<Package> existingPackages = db.getPackageRepository().findBySource(source);
             existingPackages.forEach(existingPkg -> {
               if (parsePackagesList.stream().noneMatch(p -> packageMetadataEquals(existingPkg, p))) {
                   if (existingPkg.isInstalled()) {
                      LOG.warn("Keep existing {} installed, but {} doesn't provide it anymore.", existingPkg.toStringWithNameAndVersion(), source);
                   } else {
                      LOG.info("Deleting existing {}, because {} doesn't provide it anymore.", existingPkg.toStringWithNameAndVersion(), source);
                      db.getPackageRepository().delete(existingPkg);
                   }
                   report.incRemoved();
                }
             });

            // update the timestamp
            source.setLastUpdated(LocalDateTime.now());
            db.getSourceRepository().save(source);
        }
        return report;
    }

  private boolean packageMetadataEquals(Package p1, Package p2) {

    return Objects.equals(p1.getSource(), p2.getSource()) &&
            Objects.equals(p2.getName(), p2.getName()) &&
            Objects.equals(p2.getVersion(), p2.getVersion());
  }

  private void updateProgress(ProgressReceiver progressReceiver, Source currentSource) {

        final List<Source> sources = sourcesList.getSources();
        final int sourceCount = sources.size();
        final boolean multipleSources = sourceCount != 1;

        final String message = "Updating " + currentSource.getUrl();
        final double progress;
        if (multipleSources) {
            progress = (double) sources.indexOf(currentSource) * (1d / (double) sourceCount);
        } else {
            progress = ListenableProgressFuture.INDETERMINATE;
        }
        progressReceiver.progress(message, progress);
    }
}
