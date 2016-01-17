/**
 * ****************************************************************************
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
 * ****************************************************************************
 */
package org.openthinclient.pkgmgr.connect;

import com.google.common.io.ByteStreams;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.DownloadManagerFactory;
import org.openthinclient.pkgmgr.*;
import org.openthinclient.util.dpkg.LocalPackageList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;


public class PackageListDownloader {

  public static final String PACKAGES_GZ = "Packages.gz";
  private static final Logger logger = LoggerFactory
          .getLogger(PackageListDownloader.class);
  private final PackageManagerConfiguration configuration;
  private final SourcesList sourcesList;

  public PackageListDownloader(PackageManagerConfiguration configuration, SourcesList sourcesList) {
    this.configuration = configuration;
    this.sourcesList = sourcesList;
  }

  /**
   * Download Packages.gz for each {@link Source} registered in the {@link SourcesList}
   */
  public List<LocalPackageList> checkForNewUpdatedFiles(PackageManagerTaskSummary taskSummary)
          throws PackageManagerException {
    List<LocalPackageList> updateLocalPackageList = getLines(taskSummary);
    if (updateLocalPackageList.size() == 0)
      return null;
    return updateLocalPackageList;
  }

  /**
   * @throws PackageManagerException
   */
  private List<LocalPackageList> getLines(PackageManagerTaskSummary taskSummary) throws PackageManagerException {

    return sourcesList.getSources()
            .stream()
              // we're only processing source entries, that are of type PACKAGE and enabled.
              // That is a normal debian package repository.
            .filter(Source::isEnabled)
              // for each of the sources, download the Packages.gz and process it.
              // the result will be a list of UrlAndFile entries
            .map(source -> downloadPackagesGz(taskSummary, source))
            // in case of download errors, null entries will be generated. The following line will filter these
            .filter(urlAndFile -> urlAndFile != null)
            .collect(Collectors.toList());
  }

  private LocalPackageList downloadPackagesGz(PackageManagerTaskSummary taskSummary, Source source) {
    URL packagesGZUrl = createPackagesGZUrl(source, taskSummary);

    if (packagesGZUrl == null) {
      return null;
    }

    final File targetFile = asTargetFile(packagesGZUrl);

    final DownloadManager downloadManager = DownloadManagerFactory.create(configuration.getProxyConfiguration());
    try {
      return downloadManager.download(packagesGZUrl.toURI(), in -> {

        // we're doing on-the-fly decompression of the Packages.gz file.
        in = new GZIPInputStream(in);
        try (final FileOutputStream out = new FileOutputStream(targetFile)) {
          ByteStreams.copy(in, out);
        }
        return new LocalPackageList(source, targetFile);

      });

    } catch (final Exception e) {
      // FIXME their should be a better solution!
      final String message = "URL: " + packagesGZUrl + " caused exception";

      if (null != taskSummary) {
        logger.warn(message, e);
        taskSummary.addWarning(message + "\n" + e.toString());
      } else
        logger.warn(message, e);

      return null;
    }

  }

  private File asTargetFile(URL packagesGZUrl) {

    final File listsDir = configuration.getListsDir();

    final String filename = packagesGZUrl.getHost() + "_" + packagesGZUrl.getFile().replaceAll("[/\\.-]", "_");

    return new File(listsDir, filename);
  }


  private URL createPackagesGZUrl(Source source, PackageManagerTaskSummary taskSummary) {

    final URL url = source.getUrl();

    String targetPath = url.getFile();

    // XXX at the moment we're completley disregarding the components. This is due to the fact, that the typical OTC
    // repositories do not have any components.
    final String distribution = "";

    if (!targetPath.endsWith("/") && !distribution.startsWith("/")) {
      targetPath += "/";
    }
    targetPath += distribution;
    if (!targetPath.endsWith("/")) {
      targetPath += "/";
    }
    targetPath += PACKAGES_GZ;
    try {
      return new URL(url.getProtocol(), url.getHost(), url.getPort(), targetPath);
    } catch (MalformedURLException e) {
      // it is very unlikely that this error will be thrown at all.
      final String message = I18N.getMessage("sourcesList.corrupt");
      if (null != taskSummary) {
        taskSummary.addWarning(message + "\n" + e.toString());
      }
      logger.error(message, e);

      return null;
    }

  }
}
