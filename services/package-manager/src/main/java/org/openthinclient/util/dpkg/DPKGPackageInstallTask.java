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
package org.openthinclient.util.dpkg;

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageOperationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class DPKGPackageInstallTask {

   static final Logger logger = LoggerFactory.getLogger(org.openthinclient.pkgmgr.db.Package.class);
   private final Package installablePackage;
   private final Path localPackageFile;
   public DPKGPackageInstallTask(final Package installablePackage, final Path localPackageFile) {
      this.installablePackage = installablePackage;
      this.localPackageFile = localPackageFile;
   }

   private int findAREntry(String segmentName, EntryCallback callback) throws IOException, PackageManagerException {
      final ArArchiveInputStream ais = new ArArchiveInputStream(Files.newInputStream(localPackageFile));

      ArArchiveEntry e;
      int callbackCount = 0;
      while ((e = ais.getNextArEntry()) != null)
         if (e.getName().equals(segmentName)) {
            callback.handleEntry(e.getName(), ais);
            callbackCount++;
         }

      ais.close();

      return callbackCount;
   }

   public void install(PackageOperationContext context) throws PackageManagerException {

      try {
         if (findAREntry("data.tar.gz", (entry, ais) -> {
            final TarArchiveInputStream tis = new TarArchiveInputStream(new GZIPInputStream(ais));
            TarArchiveEntry t;
            while ((t = tis.getNextTarEntry()) != null)
               installFile(tis, t, context);
         }) == 0) {
            final String errorMessage = I18N.getMessage("package.install.firstRuntimeException");
            logger.error(errorMessage);
         }
         // throw new PackageManagerException(PreferenceStoreHolder
         // .getPreferenceStoreByName("Screen").getPreferenceAsString(
         // "DPKGPackage.unableToInstall",
         // "No entry found for package.getFiles.IOException"));
      } catch (final IOException e) {
         final String errorMessage = I18N.getMessage("package.install.IOException");
         logger.error(errorMessage, e);
      }
   }

   @SuppressWarnings("unchecked") private void installFile(TarArchiveInputStream tis, TarArchiveEntry t, PackageOperationContext context)
         throws IOException, PackageManagerException {

      final Path relativePath = Paths.get(t.getName());

      if (System.getProperty("os.name").toUpperCase().contains("WINDOWS") && t.getFile().getPath().contains("::"))
         throw new IOException();

      if (t.isFile()) {
         try (final OutputStream os = context.createFile(installablePackage, relativePath)) {
            IOUtils.copy(tis, os);
         }

      } else if (t.isDirectory()) {
         context.createDirectory(installablePackage, relativePath);

      } else if (t.isLink() || t.isSymbolicLink()) {
         // FIXME shouldn't we distinguish between hard and soft links?

         context.createSymlink(installablePackage, relativePath, Paths.get(t.getLinkName()));
      }
      // FIXME warn about unknown entries!
   }

   private interface EntryCallback {

      void handleEntry(String s, InputStream inputstream) throws IOException, PackageManagerException;
   }

}
