package org.openthinclient.pkgmgr.db;

import javax.persistence.*;
import java.net.URL;

@Entity
@Table(name = "otc_source")
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

  public Long getId() {
    return id;
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

}
