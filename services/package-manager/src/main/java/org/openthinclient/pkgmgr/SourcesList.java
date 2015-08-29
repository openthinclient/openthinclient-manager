package org.openthinclient.pkgmgr;

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
