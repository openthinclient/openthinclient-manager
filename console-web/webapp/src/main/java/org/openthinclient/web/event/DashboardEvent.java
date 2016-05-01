package org.openthinclient.web.event;

import java.util.Collection;

import org.openthinclient.web.domain.Transaction;
import org.openthinclient.web.view.DashboardViewType;

/*
 * Event bus events used in Dashboard are listed here as inner classes.
 */
public abstract class DashboardEvent {

    public static final class UserLoginRequestedEvent {
      
        private final String userName, password;
        private final boolean rememberMe;

        public UserLoginRequestedEvent(final String userName, final String password, final boolean rememberMe) {
            this.userName = userName;
            this.password = password;
            this.rememberMe = rememberMe;
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

    }

    public static class BrowserResizeEvent {

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

    public static final class TransactionReportEvent {
        private final Collection<Transaction> transactions;

        public TransactionReportEvent(final Collection<Transaction> transactions) {
            this.transactions = transactions;
        }

        public Collection<Transaction> getTransactions() {
            return transactions;
        }
    }

    public static final class PostViewChangeEvent {
        private final DashboardViewType view;

        public PostViewChangeEvent(final DashboardViewType view) {
            this.view = view;
        }

        public DashboardViewType getView() {
            return view;
        }
    }

    public static class CloseOpenWindowsEvent {
    }

    public static class ProfileUpdatedEvent {
    }

}
