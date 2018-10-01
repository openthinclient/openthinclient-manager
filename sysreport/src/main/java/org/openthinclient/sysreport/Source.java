package org.openthinclient.sysreport;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;

public class Source {

  private boolean enabled;
  private URL url;

  @JsonProperty("last-updated")
  // JsonFormat with shape STRING forces the serializer to write a ISO Date string instead of an array
  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private LocalDateTime lastUpdated;

  @JsonProperty("time-since-last-update-ms")
  // JsonFormat with shape NUMBER_INT forces the serializer to write the milliseconds instead of nanoseconds
  @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
  private Duration timeSinceLastUpdate;

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

  public Duration getTimeSinceLastUpdate() {
    return timeSinceLastUpdate;
  }

  public void setTimeSinceLastUpdate(Duration timeSinceLastUpdate) {
    this.timeSinceLastUpdate = timeSinceLastUpdate;
  }
}
