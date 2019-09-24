package org.openthinclient.sysreport;

import java.nio.file.Path;

public class PackageInstalledContent {
  private Long id;

  /**
   * Stores the order in which the elements have been installed.
   */
  private Integer sequence;
  private PackageInstalledContent.Type type;
  private Path path;
  private String sha1;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public enum Type {
    FILE, DIR, SYMLINK
  }
}
