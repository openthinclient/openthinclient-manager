package org.openthinclient.pkgmgr.db;

public class PackageManagerDatabase {
    private final SourceRepository sourceRepository;
    private final PackageRepository packageRepository;
    private final InstallationRepository installationRepository;
    private final InstallationLogEntryRepository installationLogEntryRepository;
    private final PackageInstalledContentRepository installedContentRepository;

    public PackageManagerDatabase(SourceRepository sourceRepository, PackageRepository packageRepository, InstallationRepository installationRepository, InstallationLogEntryRepository installationLogEntryRepository, PackageInstalledContentRepository installedContentRepository) {
        this.sourceRepository = sourceRepository;
        this.packageRepository = packageRepository;
        this.installationRepository = installationRepository;
        this.installationLogEntryRepository = installationLogEntryRepository;
        this.installedContentRepository = installedContentRepository;
    }

    public SourceRepository getSourceRepository() {
        return sourceRepository;
    }

    public PackageRepository getPackageRepository() {
        return packageRepository;
    }

    public InstallationRepository getInstallationRepository() {
        return installationRepository;
    }

    public InstallationLogEntryRepository getInstallationLogEntryRepository() {
        return installationLogEntryRepository;
    }

    public PackageInstalledContentRepository getInstalledContentRepository() {
        return installedContentRepository;
    }
}
