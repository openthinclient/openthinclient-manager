package org.openthinclient.api.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *   popup_interval: <Stunden>
 *   popup_text_de: "<Text_DE>"
 *   popup_text_en: "<Text_EN>"
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class License {

  @JsonProperty
  private int popupInterval;
  @JsonProperty
  private String popupTextDe = "";
  @JsonProperty
  private String popupTextEn = "";

  public License() { }

  public License(int interval, String messageDE, String messageEN) {
    this.popupInterval = interval;
    this.popupTextDe = messageDE;
    this.popupTextEn = messageEN;
  }

  public License(int interval, String... messages) {
    this.popupInterval = interval;
    if (messages != null && messages.length == 2) {
      this.popupTextDe = messages[0];
      this.popupTextEn = messages[1];
    }
  }

  public int getPopupInterval() {
    return popupInterval;
  }

  public void setPopupInterval(int popupInterval) {
    this.popupInterval = popupInterval;
  }

  public String getPopupTextDe() {
    return popupTextDe;
  }

  public void setPopupTextDe(String popupTextDe) {
    this.popupTextDe = popupTextDe;
  }

  public String getPopupTextEn() {
    return popupTextEn;
  }

  public void setPopupTextEn(String popupTextEn) {
    this.popupTextEn = popupTextEn;
  }
}
