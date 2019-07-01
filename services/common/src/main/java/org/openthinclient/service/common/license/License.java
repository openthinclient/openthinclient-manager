package org.openthinclient.service.common.license;

import java.time.LocalDate;

public class License {
  String server;
  String name;
  String email;
  String details;
  Integer count;
  LocalDate softExpiredDate;
  LocalDate expiredDate;
  LocalDate createdDate;

  public enum State {
    // sorted as per https://support.openthinclient.com/openthinclient/secure/attachment/18501/Lizenz_im_Manager.png
    REQUIRED_TOO_OLD,
    REQUIRED_OLD,
    REQUIRED_EXPIRED,
    SOFT_EXPIRED,
    OK,
    INVALID,
    REQUIRED_MISSING,
    TOO_OLD,
    OLD,
    EXPIRED,
    // SOFT_EXPIRED
    // OK
    // INVALID
    // OK
  }

  public String getName() {
    return this.name;
  }
  public String getEmail() {
    return this.email;
  }
  public String getDetails() {
    return this.details;
  }
  public Integer getCount() {
    return this.count;
  }
  public LocalDate getSoftExpiredDate() {
    return this.softExpiredDate;
  }
  public LocalDate getExpiredDate() {
    return this.expiredDate;
  }
  public LocalDate getCreatedDate() {
    return this.createdDate;
  }


  public static State getState(License license, String serverID, int clientCount) {
    if(license == null) {
      return clientCount >= 50? State.REQUIRED_MISSING : State.OK;
    } else {
      return license.getState(serverID, clientCount);
    }
  }

  public State getState(String serverID, int clientCount) {
    LocalDate now = LocalDate.now();
    if(!serverID.equals(server)) {
      return State.INVALID;
    } else if(createdDate.plusDays(31).isBefore(now)) {
      return State.REQUIRED_TOO_OLD;
    } else if(createdDate.plusDays(1).isBefore(now)) {
      return clientCount >= 50? State.REQUIRED_OLD: State.OLD;
    } else if(expiredDate.isAfter(now)) {
      return clientCount >= 50? State.REQUIRED_EXPIRED: State.EXPIRED;
    } else if(softExpiredDate.isAfter(now)){
      return State.SOFT_EXPIRED;
    } else {
      return State.OK;
    }
  }
}
