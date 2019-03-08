package org.openthinclient.service.common.home;

public interface ManagerHomeMetadata {

  String getServerID();

  void setServerID(String id);

  void save();

  int getAcknowledgedPrivacyNoticeVersion();

  void setAcknowledgedPrivacyNoticeVersion(int version);
}
