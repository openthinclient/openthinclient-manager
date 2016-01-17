package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.Source;

import java.util.ArrayList;
import java.util.List;

public class SourcesList {
  private final List<Source> sources;

  public SourcesList() {
    sources = new ArrayList<>();
  }

  public List<Source> getSources() {
    return sources;
  }
}
