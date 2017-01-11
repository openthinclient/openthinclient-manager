package org.openthinclient.pkgmgr.db;

import java.nio.file.Path;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents elements that have been installed on the system during the package installation. This
 * is important to keep track which files shall be removed when there is a package uninstall.
 */
@Entity
@Table(name = "otc_package_installed_content")
public class PackageInstalledContent {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "package_id")
    private Package pkg;

    /**
     * Stores the order in which the elements have been installed.
     */
    @Column
    private Integer sequence;

    @Column(length = 10, columnDefinition = "char")
    @Enumerated(EnumType.STRING)
    private PackageInstalledContent.Type type;

    @Column
    private Path path;

    @Column(name="package")
    private String packageName;
    
    /**
     * The SHA1 checksum if the {@link #type} is {@link Type#FILE}
     */
    @Column(length = 40, columnDefinition = "char")
    private String sha1;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "PackageInstalledContent{" +
                "sequence=" + sequence +
                ", type=" + type +
                ", path=" + path +
                ", sha1='" + sha1 + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackageInstalledContent that = (PackageInstalledContent) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(pkg, that.pkg) &&
                Objects.equals(sequence, that.sequence) &&
                type == that.type &&
                Objects.equals(path, that.path) &&
                Objects.equals(sha1, that.sha1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pkg, sequence, type, path, sha1);
    }

    public Package getPackage() {
        return pkg;
    }

    public void setPackage(Package pkg) {
        this.pkg = pkg;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    /**
    * @return the packageName
    */
   public String getPackageName() {
      return packageName;
   }

   /**
    * @param packageName the packageName to set
    */
   public void setPackageName(String packageName) {
      this.packageName = packageName;
   }

   // IMPORTANT: keep in mind that the database table has a limited size to represent the type.
    public enum Type {
        FILE, DIR, SYMLINK
    }
}
