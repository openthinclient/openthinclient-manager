package org.openthinclient.sysreport;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Source {

  private boolean enabled;
  private URL url;

  @JsonProperty("last-updated")
  private Long lastUpdated;

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

  public void setUrl(URL url) {
    this.url = url;
  }

  public void setLastUpdated(LocalDateTime lastUpdated) {
    if(lastUpdated != null) {
      this.lastUpdated = lastUpdated.atZone(ZoneId.systemDefault()).toEpochSecond();
    }
  }

  public void setTimeSinceLastUpdate(Duration timeSinceLastUpdate) {
    this.timeSinceLastUpdate = timeSinceLastUpdate;
  }
}
