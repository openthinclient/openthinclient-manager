package org.openthinclient.pkgmgr.db;

import org.hibernate.annotations.GenericGenerator;

import java.util.Objects;

import javax.persistence.*;

/**
 * Represents elements that have been uninstalled but not yet deleted, as they
 * might still be in use (e.g. SFS files).
 */
@Entity
@Table(name = "otc_package_uninstalled_content")
public class PackageUninstalledContent {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO, generator="native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "package_id")
    private Long packageId;

    public Long getPackageId() {
        return packageId;
    }

    @Column
    private Integer sequence;

    @Column
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public PackageUninstalledContent() {
    }

    public PackageUninstalledContent(PackageInstalledContent pic) {
        this.packageId = pic.getPackage().getId();
        this.sequence = pic.getSequence();
        this.path = pic.getPath().toString();
    }

    @Override
    public String toString() {
        return String.format("PackageUninstalledContent{id=%d, path=%s}",
                             id, path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackageUninstalledContent that = (PackageUninstalledContent) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, path);
    }
}
