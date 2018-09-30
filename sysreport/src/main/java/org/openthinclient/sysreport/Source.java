package org.openthinclient.sysreport;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;
import java.time.LocalDateTime;

public class Source {

  private boolean enabled;
  private URL url;
  @JsonProperty("last-updated")
  private LocalDateTime lastUpdated;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public URL getUrl() {
    return url;
  }

  public void setUrl(URL url) {
    this.url = url;
  }

  public LocalDateTime getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(LocalDateTime lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
}
