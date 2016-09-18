package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.Source;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
public class SourcesList {
  @XmlElement(name = "source")
  private final List<Source> sources;

  public SourcesList() {
    sources = new ArrayList<>();
  }

  public List<Source> getSources() {
    return sources;
  }
}
