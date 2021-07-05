package org.openthinclient.web.event;

import com.vaadin.ui.Label;

import java.util.List;

import org.openthinclient.common.model.DirectoryObject;

public abstract class DashboardEvent {

  public static final class UserLoginRequestedEvent {

    private final String userName, password;
    private final boolean rememberMe;
    private final Label loginFailedLabel;

    public UserLoginRequestedEvent(final String userName, final String password,
    final boolean rememberMe, final Label loginFailedLabel) {
      this.userName = userName;
      this.password = password;
      this.rememberMe = rememberMe;
      this.loginFailedLabel = loginFailedLabel;
    }

    public String getUserName() {
      return userName;
    }

    public String getPassword() {
      return password;
    }

    public boolean isRememberMe() {
      return rememberMe;
    }

    public Label getLoginFailedLabel() {
      return loginFailedLabel;
    }
  }

  public static class BrowserResizeEvent {

    private final int height;
    private final int width;

    public BrowserResizeEvent(int height, int width) {
      this.height = height;
      this.width = width;
    }

    public int getHeight() {
      return height;
    }

    public int getWidth() {
      return width;
    }
  }

  public static class NotificationsCountUpdatedEvent {
  }

  public static final class ReportsCountUpdatedEvent {

    private final int count;

    public ReportsCountUpdatedEvent(final int count) {
      this.count = count;
    }

    public int getCount() {
      return count;
    }
  }

  public static class CloseOpenWindowsEvent {
  }

  public static class ProfileUpdatedEvent {
  }

  public static class PXEClientListRefreshEvent {

    private List<?> items;

    public PXEClientListRefreshEvent(List<?> items) {
      this.items = items;
    }

    public List<?> getItems() {
      return items;
    }
  }

  public static class SearchObjectsSetupEvent {

    List<DirectoryObject> directoryObjects;

    public SearchObjectsSetupEvent(List<DirectoryObject> directoryObjects) {
      this.directoryObjects = directoryObjects;
    }

    public List<DirectoryObject> getDirectoryObjects() {
      return directoryObjects;
    }
  }

  public static class ClientCountChangeEvent {
  }

  public static class LDAPImportEvent extends ClientCountChangeEvent {
  }

}
