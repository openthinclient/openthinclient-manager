package org.openthinclient.service.common.home;

public interface ManagerHomeMetadata {

  String getServerID();

  void setServerID(String id);

  /**
   * Whether or not usage statistic generation and transmission is enabled or not. This value may
   * not be changed by any logic. Instead the administrator is required to manually configure the
   * setting.
   * @return {@code true} (default) if anonymous usage statistics shall be generated and transmitted
   */
  boolean isUsageStatisticsEnabled();

  void save();
}
