/*******************************************************************************
 * openthinclient.org ThinClient suite
 *
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 *
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.util.dpkg;

import org.apache.commons.io.FileSystemUtils;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.I18N;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.PackageManagerTaskSummary;
import org.openthinclient.pkgmgr.SourcesList;
import org.openthinclient.pkgmgr.UpdateDatabase;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageInstalledContent;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.exception.SourceIntegrityViolationException;
import org.openthinclient.pkgmgr.op.DefaultPackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperationReport;
import org.openthinclient.pkgmgr.op.PackageManagerOperationTask;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;


/**
 * This is the Heart of the whole PackageManger all the different Databases
 * Files and Packages are handeld in here. nearly every interaction is started
 * here.
 *
 * @author levigo
 */
public class DPKGPackageManager implements PackageManager {

    private static final Logger logger = LoggerFactory.getLogger(DPKGPackageManager.class);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final File installDir;
    private final File archivesDir;
    private final File testinstallDir;
    private final File oldInstallDir;
    private final File listsDir;
    private final PackageManagerConfiguration configuration;
    private final List<Package> pack = new LinkedList<Package>();
    private final List<Package> packForDelete = new LinkedList<>();
    private final PackageManagerDatabase packageManagerDatabase;
    private final PackageManagerExecutionEngine executionEngine;
    private final LocalPackageRepository localPackageRepository;
    private final List<PackagingConflict> conflicts = new ArrayList<PackagingConflict>();
    private PackageManagerTaskSummary taskSummary = new PackageManagerTaskSummary();
    private HashMap<File, File> fromToFileMap;
    private List<File> removeDirectoryList;
    private final DownloadManager downloadManager;

    public DPKGPackageManager(PackageManagerConfiguration configuration, PackageManagerDatabase packageManagerDatabase, PackageManagerExecutionEngine executionEngine, DownloadManager downloadManager) {
        this.configuration = configuration;
        this.packageManagerDatabase = packageManagerDatabase;
        this.executionEngine = executionEngine;

        this.localPackageRepository = new DefaultLocalPackageRepository(configuration.getArchivesDir().toPath());

        this.installDir = configuration.getInstallDir();
        this.archivesDir = configuration.getArchivesDir();
        this.testinstallDir = configuration.getTestinstallDir();
        this.oldInstallDir = configuration.getInstallOldDir();
        this.listsDir = configuration.getListsDir();

        this.downloadManager = downloadManager;
    }

    @Override
    public PackageManagerConfiguration getConfiguration() {
        return configuration;
    }

    public void close() throws PackageManagerException {

        // nothing to do right now
    }

    private File relativeFile(File baseDirectory, File absoluteFile) {

        final Path basePath = baseDirectory.getAbsoluteFile().toPath();
        final Path absolutePath = absoluteFile.getAbsoluteFile().toPath();

        return basePath.relativize(absolutePath).toFile();

    }

    /**
     * Check if the given file represents the "root" entry relative to the given baseDirectory.
     *
     * @param baseDirectory the directory serving as a "virtual root"
     * @param file          the file to be compared agains the base dir
     * @return <code>true</code> if the file represents the relative root
     */
    private boolean isRoot(File baseDirectory, File file) {
        return baseDirectory.equals(file);
    }


    /**
     * adds a dependency to the existent ones
     *
     * @param unsatisfiedDependencies
     * @param r
     * @param toBeInstalled
     */
    private void addDependency(Map<PackageReference, List<Package>> unsatisfiedDependencies, PackageReference r, Package toBeInstalled) {
        if (!unsatisfiedDependencies.containsKey(r))
            unsatisfiedDependencies.put(r, new LinkedList<>());
        unsatisfiedDependencies.get(r).add(toBeInstalled);
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Package> getInstallablePackages() {
        return packageManagerDatabase.getPackageRepository().findInstallablePackages();
    }

    public Collection<Package> getInstalledPackages() {
        return packageManagerDatabase.getPackageRepository().findByInstalledTrue();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Package> getInstallablePackagesWithoutInstalledOfSameVersion() {
        Collection<Package> installablePackages = getInstallablePackages();
        Collection<Package> installedPackages = getInstalledPackages();
        installablePackages.removeAll(installedPackages);
        return installablePackages;
    }

    @SuppressWarnings("unchecked")
    public Collection<Package> getUpdateablePackages() {
        ArrayList<Package> update = new ArrayList<>();
        for (final Package installedPkg : getInstalledPackages()) {
            for (final Package installablePkg : getInstallablePackagesWithoutInstalledOfSameVersion()) {
                if (installablePkg.getName().equals(installedPkg.getName()) && installablePkg.getVersion().compareTo(installedPkg.getVersion()) == 1) {
                    update.add(installablePkg);
                }
            }
        }
        return update;
    }

    /**
     * return the actually formatted date type in YYYY_MM_DD_HH_MM_ss
     *
     * @return String the actually formatted date
     */
    private String getFormattedDate() {
        final SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy'_'MM'_'dd'_'HH'_'mm'_'ss");
        return sdf.format(new GregorianCalendar().getTime());
    }


    public long getFreeDiskSpace() throws PackageManagerException {
        try {
            return FileSystemUtils.freeSpaceKb(installDir.getAbsolutePath());
        } catch (final IOException io) {
            io.printStackTrace();
            addWarning(io.toString());
            logger.error("Failed to access free disk space information", io);
            return 0;

            // throw new PackageManagerException(io);

        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> getChangelogFile(Package p) throws IOException {
        throw new UnsupportedOperationException("This operation is not supported at the moment");
        //		final String name = (new File((new StringBuilder()).append(listsDir)
        //				.append(File.separator).append(p.getChangelogDir()).toString(),
        //				(new StringBuilder()).append(p.getName()).append(".changelog")
        //						.toString())).getCanonicalPath();
        //		if (!new File(name).isFile())
        //			return new ArrayList<String>(Collections.EMPTY_LIST);
        //		InputStream stream = null;
        //		BufferedReader br = null;
        //		if ((new File(name)).length() != 0L)
        //			stream = new FileInputStream(name);
        //		if (stream == null) {
        //			final ClassLoader aClassLoader = getClass()
        //					.getClassLoader();
        //			if (aClassLoader == null)
        //				stream = ClassLoader.getSystemResourceAsStream(name);
        //			else
        //				stream = aClassLoader.getResourceAsStream(name);
        //		}
        //		if (stream == null)
        //			return null;
        //		br = new BufferedReader(new InputStreamReader(stream));
        //		final ArrayList<String> lines = new ArrayList<String>();
        //		String line;
        //		while ((line = br.readLine()) != null)
        //			lines.add(line);
        //		br.close();
        //		stream.close();
        //		return lines;
    }


    public HashMap<File, File> getFromToFileMap() {
        return fromToFileMap;
    }

    public ListenableProgressFuture<PackageListUpdateReport> updateCacheDB() {
        return executionEngine.enqueue(new UpdateDatabase(configuration, getSourcesList(), packageManagerDatabase, downloadManager));
    }

    public ListenableProgressFuture<PackageListUpdateReport> deleteSourcePackagesFromCacheDB(Source source) {
       return executionEngine.enqueue(new RemoveFromDatabase(configuration, source, packageManagerDatabase));
   }
    
    public boolean addWarning(String warning) {
        taskSummary.addWarning(warning);
        return true;
    }

    /**
     * Apply a {@link PackageManagerTaskSummary} instance to this
     * {@link DPKGPackageManager} instance. This will effectively override any
     * exiting {@link PackageManagerTaskSummary} instance associated with this
     * {@link DPKGPackageManager}.
     *
     * @param taskSummary
     */
    public void setTaskSummary(PackageManagerTaskSummary taskSummary) {
        if (taskSummary == null)
            throw new IllegalArgumentException("taskSummary must not be null");
        this.taskSummary = taskSummary;
    }

    public PackageManagerTaskSummary fetchTaskSummary() {
        PackageManagerTaskSummary result = taskSummary;
        taskSummary = new PackageManagerTaskSummary();
        return result;
    }

    @Override
    public SourcesList getSourcesList() {
        final SourcesList sourcesList = new SourcesList();
        sourcesList.getSources().addAll(packageManagerDatabase.getSourceRepository().findAll());
        return sourcesList;
    }

    @Override
    public boolean isInstalled(Package pkg) {

        Package dbPackage = packageManagerDatabase.getPackageRepository().getBySourceAndNameAndVersion(pkg.getSource(), pkg.getName(), pkg.getVersion());

        return dbPackage != null && dbPackage.isInstalled();

    }

    @Override
    public boolean isInstallable(Package pkg) {
        Package dbPackage = packageManagerDatabase.getPackageRepository().getBySourceAndNameAndVersion(pkg.getSource(), pkg.getName(), pkg.getVersion());

        return dbPackage != null && !dbPackage.isInstalled();

    }

    @Override
    public LocalPackageRepository getLocalPackageRepository() {
        return localPackageRepository;
    }

    @Override
    public PackageManagerOperation createOperation() {
        return new DefaultPackageManagerOperation(
                new PackageManagerOperationResolverImpl(this::getInstalledPackages, this::getInstallablePackages));
    }

    @Override
    public ListenableProgressFuture<PackageManagerOperationReport> execute(PackageManagerOperation operation) {

        if (operation == null)
            throw new IllegalArgumentException("operation must not be null");

        if (!(operation instanceof DefaultPackageManagerOperation))
            throw new IllegalArgumentException("The provided package manager operation is unsupported. (" + operation.getClass().getName() + ")");

        return executionEngine.enqueue(new PackageManagerOperationTask(configuration, operation.getInstallPlan(), packageManagerDatabase, localPackageRepository, downloadManager));

    }

    public enum DeleteMode {
        OLDINSTALLDIR,
        INSTALLDIR
    }

    // subclass PackagingConflict
    // Eine Klasse zur reinen Ausgabe der Fehler die Unterschiedlich
    // angesprochen werden kann und mit der Methode toString()
    // einen String zurueckliefert
    public static class PackagingConflict {
        private final Type type;
        private final Package pkg;
        private Package conflicting;
        private List<Package> pkgs;
        private PackageReference ref;
        private File file;

        // Konstruktoren
        public PackagingConflict(Type type, Package pkg) {
            this.type = type;
            this.pkg = pkg;
        }

        public PackagingConflict(Type type, Package pkg, Package conflicting) {
            this(type, pkg);
            this.conflicting = conflicting;
        }

        public PackagingConflict(Type type, PackageReference ref, List<Package> pkgs) {
            this(type, null);
            this.ref = ref;
            this.pkgs = pkgs;
        }

        public PackagingConflict(Type type, Package pkg, Package conflicting, File f) {
            this(type, pkg, conflicting);
            this.file = f;
        }

        public Package getConflictingPackage() {
            return this.pkg;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            switch (type) {
                case ALREADY_INSTALLED:
                    sb.append(pkg).append(" ");
                    // .append(
                    // PreferenceStoreHolder// .getPreferenceStoreByName("Screen")// .getPreferenceAsString(
                    // "packageManager.toString.ALREADY_INSTALLED",
                    // "No entry found for packageManager.toString.ALREADY_INSTALLED"));

                    break;
                case CONFLICT_EXISTING:
                    sb.append(pkg.forConflictsToString()).append(" ")
                            // .append(
                            // PreferenceStoreHolder// .getPreferenceStoreByName("Screen")// .getPreferenceAsString(
                            // "packageManager.toString.CONFLICT_EXISTING",
                            // "No entry found for
                            // packageManager.toString.CONFLICT_EXISTING"))
                            .append(" ").append(conflicting.forConflictsToString());
                    break;
                case CONFLICT_NEW:
                    sb.append(pkg.forConflictsToString()).append(" ")
                            // .append(
                            // PreferenceStoreHolder// .getPreferenceStoreByName("Screen")// .getPreferenceAsString(
                            // "packageManager.toString.ALREADY_INSTALLED",
                            // "No entry found for
                            // packageManager.toString.ALREADY_INSTALLED"))
                            .append(" ").append(conflicting.forConflictsToString());
                    break;
                case UNSATISFIED:
                    sb
                            // .append(
                            // PreferenceStoreHolder// .getPreferenceStoreByName("Screen")// .getPreferenceAsString(
                            // "packageManager.toString.ALREADY_INSTALLED",
                            // "No entry found for
                            // packageManager.toString.ALREADY_INSTALLED"))
                            .append(" ").append(ref).append(" ")
                            // .append(
                            // PreferenceStoreHolder// .getPreferenceStoreByName("Screen")// .getPreferenceAsString(
                            // "packageManager.toString.ALREADY_INSTALLED",
                            // "No entry found for
                            // packageManager.toString.ALREADY_INSTALLED"))
                            .append(" ");
                    for (final Package pkg : pkgs)
                        sb.append(pkg.getName()).append(" ");
                    break;
                case FILE_CONFLICT:
                    sb
                            .append(
                                    I18N.getMessage(
                                            "packageManager.toString.ALREADY_INSTALLED"))
                            .append(" ")
                            .append(file)
                            .append(" ")
                            .append(
                                    I18N.getMessage(
                                            "packageManager.toString.ALREADY_INSTALLED"))
                            .append(" ")
                            .append(pkg)
                            .append(" ")
                            .append(
                                    I18N.getMessage(
                                            "packageManager.toString.ALREADY_INSTALLED"))
                            .append(" ").append(conflicting);
                    break;
            }

            return sb.toString();
        }

        public enum Type {
            ALREADY_INSTALLED, CONFLICT_EXISTING, CONFLICT_NEW, UNSATISFIED, FILE_CONFLICT, CONFLICT_WITHIN
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSource(Source source) throws SourceIntegrityViolationException {
      // to take care of ConstraintViolationException check if there is no installed package of this source
      List<Package> list = getInstalledPackages().stream()
                                                 .filter(p -> p.getSource().equals(source))
                                                 .collect(Collectors.toList());
      if (list.isEmpty()) {
        packageManagerDatabase.getSourceRepository().delete(source);
      } else {
        throw new SourceIntegrityViolationException("Cannot delete source, because there are installed packages of this source", list);
      }
      
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Source saveSource(Source source) {
      return packageManagerDatabase.getSourceRepository().saveAndFlush(source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Source> findAllSources() {
       return packageManagerDatabase.getSourceRepository().findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSources(List<Source> sources) {
      packageManagerDatabase.getSourceRepository().save(sources);
    }

  @Override
  public List<PackageInstalledContent> getInstalledPackageContents(Package pkg) {
    return packageManagerDatabase.getInstalledContentRepository().findByPkgOrderBySequenceDesc(pkg);
  }
}
