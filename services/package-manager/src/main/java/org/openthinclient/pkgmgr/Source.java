package org.openthinclient.pkgmgr;

import javax.persistence.*;
import java.net.URL;

@Entity
@Table(name = "SOURCE")
public class Source {

  @Id
  @GeneratedValue
  private Long id;

  @Column(name = "ENABLED")
  private boolean enabled;
  @Column(name = "DESCRIPTION")
  private String description;
  @Column(name = "URL")
  private URL url;

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

}
