package org.openthinclient.pkgmgr.exception;

import java.util.List;

import org.openthinclient.pkgmgr.db.Package;

/**
 * SourceIntegrityViolationException will be throws if a source cannot be deleted because of already installed packages from this source
 * @author JN
 */
public class SourceIntegrityViolationException extends Exception {

  private static final long serialVersionUID = 5395688585293081999L;

  private List<Package> packages;

  public SourceIntegrityViolationException(String message, List<Package> packages) {
    super(message);
    this.packages = packages;
  }

  public List<Package> getPackages() {
    return packages;
  }

}
