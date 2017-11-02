package org.openthinclient.web.event;

import com.vaadin.ui.Component;

/*
 * Event bus events used in Dashboard are listed here as inner classes.
 */
public abstract class DashboardEvent {

    public static final class UserLoginRequestedEvent {
      
        private final String userName, password;
        private final boolean rememberMe;
        private final Component fields;

        public UserLoginRequestedEvent(final String userName, final String password, final boolean rememberMe, final Component fields) {
            this.userName = userName;
            this.password = password;
            this.rememberMe = rememberMe;
            this.fields = fields;
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

        public Component getFields() {
            return fields;
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

    public static class UserLoggedOutEvent {

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

}
