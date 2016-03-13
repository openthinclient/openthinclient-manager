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

import org.openthinclient.manager.util.http.DownloadManagerFactory;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.pkgmgr.connect.PackageListDownloader;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.openthinclient.pkgmgr.progress.ProgressReceiver;
import org.openthinclient.pkgmgr.progress.ProgressTask;
import org.openthinclient.util.dpkg.LocalPackageList;
import org.openthinclient.util.dpkg.PackagesListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class UpdateDatabase implements ProgressTask<PackageListUpdateReport> {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateDatabase.class);
    private final PackageManagerConfiguration configuration;
    private final SourcesList sourcesList;
    private final PackageRepository packageRepository;
    private final PackageManagerDirectoryStructure directoryStructure;

    public UpdateDatabase(PackageManagerConfiguration configuration, SourcesList sourcesList, PackageRepository packageRepository) {
        this.configuration = configuration;
        this.sourcesList = sourcesList;
        this.packageRepository = packageRepository;
        this.directoryStructure = new PackageManagerDirectoryStructureImpl(configuration);
    }

    private static URL createPackageChangeLogURL(String serverPath, Package pkg) {
        try {
            return new URL(serverPath + pkg.getName() + ".changelog");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to access changelog due to illegal url", e);
        }
    }

    private boolean downloadChangelogFile(NetworkConfiguration.ProxyConfiguration proxyConfiguration, Source source, Package pkg,
                                          PackageManagerTaskSummary taskSummary)
            throws PackageManagerException {
        try {
            // server path is the base url to the package repository.
            String serverPath = pkg.getServerPath();
            if (!serverPath.endsWith("/")) {
                serverPath = serverPath + "/";
            }

            final Path changelogFile = directoryStructure.changelogFileLocation(source, pkg);
            Files.createDirectories(changelogFile.getParent());

            DownloadManagerFactory.create(proxyConfiguration).downloadTo(createPackageChangeLogURL(serverPath, pkg), changelogFile.toFile());

            return true;
        } catch (final Exception e) {
            if (null != taskSummary) {
                taskSummary.addWarning(e.toString());
            }
            LOG.warn("Changelog download failed.", e);
            return false;
        }
    }

    private void addPackage(Package pkg, PackageListUpdateReport report) {

        final Package existing = packageRepository.getByNameAndVersion(pkg.getName(), pkg.getVersion());

        if (existing != null) {
            LOG.info("Skipping already existing package {} {}", pkg.getName(), pkg.getVersion());
        } else {

            LOG.info("Adding new package {} {}", pkg.getName(), pkg.getVersion());
            packageRepository.save(pkg);

            report.incAdded();
        }
    }

    private Stream<Package> parsePackagesList(LocalPackageList localPackageList) {
        LOG.info("Processing packages for {}", localPackageList.getSource().getUrl());

        try {
            return new PackagesListParser()
                    .parse(Files.newInputStream(localPackageList.getPackagesFile().toPath()))
                    .stream()
                    .map(p -> {
                        // FIXME add the source here as well
                        p.setServerPath(localPackageList.getSource().getUrl().toExternalForm());
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
    public PackageListUpdateReport execute(ProgressReceiver progressReceiver) throws Exception {

        final PackageListDownloader packageListDownloader = new PackageListDownloader(configuration, DownloadManagerFactory.create(configuration.getProxyConfiguration()));

        final PackageListUpdateReport report = new PackageListUpdateReport();

        for (Source source : sourcesList.getSources()) {

            if (!source.isEnabled())
                // FIXME this will cause invalid progress values
                continue;

            updateProgress(progressReceiver, source);

            final LocalPackageList localPackageList = packageListDownloader.download(source);
            parsePackagesList(localPackageList).forEach((pkg) -> addPackage(pkg, report));

        }
        return report;
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
