package org.openthinclient.pkgmgr;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Source {

  private final List<String> components;
  private boolean enabled;
  private Type type;
  private String description;
  private URL url;
  private String distribution;

  public Source() {
    components = new ArrayList<>();
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public URL getUrl() {
    return url;
  }

  public void setUrl(URL url) {
    this.url = url;
  }

  public String getDistribution() {
    return distribution;
  }

  public void setDistribution(String distribution) {
    this.distribution = distribution;
  }

  public List<String> getComponents() {
    return components;
  }

  public static enum Type {
    /**
     * Representing "deb"-Lines
     */
    PACKAGE,
    /**
     * Representing "deb-src"-Lines
     */
    PACKAGE_SOURCE
  }

}
