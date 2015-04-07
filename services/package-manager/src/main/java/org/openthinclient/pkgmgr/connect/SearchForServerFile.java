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

import org.apache.commons.io.IOUtils;
import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.openthinclient.pkgmgr.Source;
import org.openthinclient.pkgmgr.SourcesList;
import org.openthinclient.util.dpkg.UrlAndFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;


public class SearchForServerFile {

  private static final Logger logger = LoggerFactory
          .getLogger(SearchForServerFile.class);
  public static final String PACKAGES_GZ = "Packages.gz";
  private final PackageManagerConfiguration configuration;
  private final SourcesList sourcesList;

  public SearchForServerFile(PackageManagerConfiguration configuration, SourcesList sourcesList) {
    this.configuration = configuration;
    this.sourcesList = sourcesList;
  }

  /**
   * loads the Packages.gz files out of the sources.list, and download this
   * files, and save it to the disk.
   *
   * @return List<UrlAndFile>
   * @throws PackageManagerException
   */
  public List<UrlAndFile> checkForNewUpdatedFiles(PackageManagerTaskSummary taskSummary)
          throws PackageManagerException {
    List<UrlAndFile> updateUrlAndFile = getLines(taskSummary);
    if (updateUrlAndFile.size() == 0) {
      updateUrlAndFile = null;
      return updateUrlAndFile;
    } else
      return updateUrlAndFile;

  }

  /**
   * @throws InterruptedException
   * @throws PackageManagerException
   */
  private List<UrlAndFile> getLines(PackageManagerTaskSummary taskSummary) throws PackageManagerException {

    // this means that the line which is significant for us should look like
    // this:
    // " deb hereStandsAnUrl hereStandsWhichFolder
    ArrayList<UrlAndFile> updateUrlAndFile = new ArrayList<>();

    return sourcesList.getSources()
            .stream()
              // we're only processing source entries, that are of type PACKAGE and enabled.
              // That is a normal debian package repository.
            .filter(source -> source.getType() == Source.Type.PACKAGE && source.isEnabled())
              // for each of the sources, download the Packages.gz and process it.
              // the result will be a list of UrlAndFile entries
            .map(source -> downloadPackagesGz(taskSummary, source))
            // in case of download errors, null entries will be generated. The following line will filter these
            .filter(urlAndFile -> urlAndFile != null)
            .collect(Collectors.toList());
  }

  private UrlAndFile downloadPackagesGz(PackageManagerTaskSummary taskSummary, Source source) {
    URL packagesGZUrl = createPackagesGZUrl(source, taskSummary);

    if (packagesGZUrl == null) {
      return null;
    }

    String changelogdir = asChangelogDirectoryName(packagesGZUrl);

    final File targetFile = asTargetFile(packagesGZUrl);
    try (final GZIPInputStream in = openPackagesGzStream(packagesGZUrl, taskSummary);
         final FileOutputStream out = new FileOutputStream(targetFile)) {

      IOUtils.copy(in, out);
      out.close();
      in.close();
      return new UrlAndFile(packagesGZUrl.getProtocol() + "://" + packagesGZUrl.getHost(), targetFile,
              changelogdir);

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

  private GZIPInputStream openPackagesGzStream(URL packagesGZUrl, PackageManagerTaskSummary taskSummary) throws IOException, PackageManagerException {
    return new GZIPInputStream(new ConnectToServer(configuration.getProxyConfiguration(),
            taskSummary).getInputStream(packagesGZUrl));
  }

  private String asChangelogDirectoryName(URL packagesGZUrl) {
    // creating the initial changelog directory as a filename constructed using the host and the realtive path to the Packages.gz (without the Packages.gz itself)
    String changelogdir = packagesGZUrl.getHost() + "_" + packagesGZUrl.getFile().replace(PACKAGES_GZ, "");
    if (changelogdir.endsWith("/"))
      changelogdir = changelogdir.substring(0, changelogdir
              .lastIndexOf("/"));
    changelogdir = changelogdir.replace('/', '_');
    changelogdir = changelogdir.replaceAll("\\.", "_");
    changelogdir = changelogdir.replaceAll("-", "_");
    changelogdir = changelogdir.replaceAll(":", "_COLON_");
    return changelogdir;
  }

  private URL createPackagesGZUrl(Source source, PackageManagerTaskSummary taskSummary) {

    final URL url = source.getUrl();

    String targetPath = url.getFile();

    // XXX at the moment we're completley disregarding the components. This is due to the fact, that the typical OTC
    // repositories do not have any components.

    String distribution = source.getDistribution();
    if (distribution == null || distribution.equals("./")) {
      distribution = "";
    }

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
