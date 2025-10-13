package org.openthinclient.pkgmgr.db;

public class PackageManagerDatabase {
    private final SourceRepository sourceRepository;
    private final PackageRepository packageRepository;
    private final PackageInstalledContentRepository installedContentRepository;

    public PackageManagerDatabase(SourceRepository sourceRepository, PackageRepository packageRepository, PackageInstalledContentRepository installedContentRepository) {
        this.sourceRepository = sourceRepository;
        this.packageRepository = packageRepository;
        this.installedContentRepository = installedContentRepository;
    }

    public SourceRepository getSourceRepository() {
        return sourceRepository;
    }

    public PackageRepository getPackageRepository() { return packageRepository; }

    public PackageInstalledContentRepository getInstalledContentRepository() {
        return installedContentRepository;
    }

    /**
     * TODO: remove this when spring-boot-data 2.x is used
     * This is a workaround for handling 'is null' of a value (especially 'debian_revision')
     * @param source - Source
     * @param name - package name
     * @param version - package version
     * @return Package or null
     */
    public Package getBySourceAndNameAndVersion(Source source, String name, Version version) {
        if (version != null && version.getDebianRevision() != null) {
            return getPackageRepository().getBySourceAndNameAndVersionWithRevision(source, name, version.getEpoch(), version.getUpstreamVersion(), version.getDebianRevision());
        } else {
            return getPackageRepository().getBySourceAndNameAndVersionWithRevisionIsNull(source, name, version.getEpoch(), version.getUpstreamVersion());
        }
    }
}
