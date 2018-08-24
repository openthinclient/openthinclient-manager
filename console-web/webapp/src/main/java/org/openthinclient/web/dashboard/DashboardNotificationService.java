package org.openthinclient.web.dashboard;

import org.openthinclient.web.domain.DashboardNotification;

import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * A service providing the UI with relevant notifications for the user.
 */
public interface DashboardNotificationService {

  List<DashboardNotification> getNotifications();

  int getUnreadNotificationsCount();

  /**
   * Implementation that will always return an empty list. Currently, notifications are not used in
   * the implementation. This class serves as a placeholder for future use.
   */
  class Dummy implements DashboardNotificationService {

    @Override
    public List<DashboardNotification> getNotifications() {
      return Collections.emptyList();
    }

    @Override
    public int getUnreadNotificationsCount() {
      return 2;
    }
  }
}
