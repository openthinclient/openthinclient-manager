/*******************************************************************************
 * openthinclient.org ThinClient suite
 * <p/>
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * <p/>
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.pkgmgr;

import org.openthinclient.manager.util.http.DownloadManagerFactory;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.pkgmgr.connect.PackageListDownloader;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.util.dpkg.LocalPackageList;
import org.openthinclient.util.dpkg.PackagesListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * removes the actually cache database, connect to the internet load the newest
 * Packages.gz files (which are given in the sources.list file) and make a new
 * database out of the Packages which are in these files, and not already
 * installed.
 *
 * @author tauschfn
 *
 */
public class UpdateDatabase {

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

    public void doUpdate(PackageManagerTaskSummary taskSummary, NetworkConfiguration.ProxyConfiguration proxyConfiguration)
            throws PackageManagerException {

        final PackageListDownloader seFoSeFi = new PackageListDownloader(configuration, sourcesList);


        final Stream<LocalPackageList> sourcesPackagesListStream = seFoSeFi.checkForNewUpdatedFiles(taskSummary).stream();

        sourcesPackagesListStream.flatMap(this::parsePackagesList).forEach(this::addPackage);

    }

    private void addPackage(Package pkg) {

        final Package existing = packageRepository.getByNameAndVersion(pkg.getName(), pkg.getVersion());

        if (existing != null) {
            LOG.info("Skipping already existing package {} {}", pkg.getName(), pkg.getVersion());
        } else {

            LOG.info("Adding new package {} {}", pkg.getName(), pkg.getVersion());
            packageRepository.save(pkg);

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

}
