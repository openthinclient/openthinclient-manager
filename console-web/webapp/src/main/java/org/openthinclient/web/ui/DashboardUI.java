package org.openthinclient.web.ui;

import com.google.common.eventbus.Subscribe;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.web.data.DataProvider;
import org.openthinclient.web.data.dummy.DummyDataProvider;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.event.DashboardEvent.BrowserResizeEvent;
import org.openthinclient.web.event.DashboardEvent.CloseOpenWindowsEvent;
import org.openthinclient.web.event.DashboardEvent.UserLoggedOutEvent;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.view.MainView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.security.VaadinSecurity;
import org.vaadin.spring.sidebar.components.ValoSideBar;

import java.util.Locale;

@Theme("dashboard")
//@Widgetset("org.openthinclient.web.DashboardWidgetSet")
@Title("openthinclient.org")
@SpringUI(path = "/")
//@Push
public final class DashboardUI extends UI {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardUI.class);
    /*
     * This field stores an access to the dummy backend layer. In real
     * applications you most likely gain access to your beans trough lookup or
     * injection; and not in the UI but somewhere closer to where they're
     * actually accessed.
     */
    private final DataProvider dataProvider = new DummyDataProvider();
    private final DashboardEventBus dashboardEventbus = new DashboardEventBus();
    @Autowired
    VaadinSecurity vaadinSecurity;
    @Autowired
    SpringViewProvider viewProvider;
    @Autowired
    ValoSideBar sideBar;
    @Autowired
    private EventBus.SessionEventBus eventBus;

    /**
     * @return An instance for accessing the (dummy) services layer.
     */
    public static DataProvider getDataProvider() {
        return ((DashboardUI) getCurrent()).dataProvider;
    }

    public static DashboardEventBus getDashboardEventbus() {
        return ((DashboardUI) getCurrent()).dashboardEventbus;
    }

    @Override
    protected void init(final VaadinRequest request) {
        setLocale(Locale.US);

        DashboardEventBus.register(this);
        Responsive.makeResponsive(this);
        addStyleName(ValoTheme.UI_WITH_MENU);

        updateContent();

        // Some views need to be aware of browser resize events so a
        // BrowserResizeEvent gets fired to the event bus on every occasion.
        Page.getCurrent().addBrowserWindowResizeListener(event -> DashboardEventBus.post(new BrowserResizeEvent()));

    }

    /**
     * Updates the correct content for this UI based on the current user status.
     * If the user is logged in with appropriate privileges, main view is shown.
     * Otherwise login view is shown.
     */
    private void updateContent() {
//        User user = (User) VaadinSession.getCurrent().getAttribute(User.class.getName());
    	Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ) {
            // Authenticated user
            setContent(new MainView(viewProvider, sideBar));
            removeStyleName("loginview");
            getNavigator().navigateTo("dashboard");
        } else {
        	// TODO: redirect to login
            setContent(new LoginView(eventBus));
            addStyleName("loginview");
        }
    }

    @EventBusListenerMethod
    public void userLoginRequested(final DashboardEvent.UserLoginRequestedEvent event) {
//        User user = getDataProvider().authenticate(event.getUserName(), event.getPassword());

		try {
			final Authentication authentication = vaadinSecurity.login(event.getUserName(), event.getPassword());
//            eventBus.publish(this, new SuccessfulLoginEvent(getUI(), authentication));
	        updateContent();
		} catch (AuthenticationException ex) {
//			userName.focus();
//			userName.selectAll();
//			passwordField.setValue("");
//			loginFailedLabel.setValue(String.format("Login failed: %s",
//					ex.getMessage()));
//			loginFailedLabel.setVisible(true);
//			if (loggedOutLabel != null) {
//				loggedOutLabel.setVisible(false);
//			}
			Notification.show("Login failed", ex.getMessage(), Notification.Type.ERROR_MESSAGE);
		} catch (Exception ex) {
			Notification.show("An unexpected error occurred", ex.getMessage(),
					Notification.Type.ERROR_MESSAGE);
			LOGGER.error("Unexpected error while logging in", ex);
		} finally {
//			login.setEnabled(true);
		}
	}

    @Subscribe
    public void userLoggedOut(final UserLoggedOutEvent event) {
        // When the user logs out, current VaadinSession gets closed and the
        // page gets reloaded on the login screen. Do notice the this doesn't
        // invalidate the current HttpSession.
        VaadinSession.getCurrent().close();
        SecurityContextHolder.getContext().setAuthentication(null); // TODO JN: Hä?? muss doch folgende Zeile tun...
        vaadinSecurity.logout();
        Page.getCurrent().reload();
    }

    @Subscribe
    public void closeOpenWindows(final CloseOpenWindowsEvent event) {
        for (Window window : getWindows()) {
            window.close();
        }
    }
    
    @Override
    public void attach() {
        super.attach();
        eventBus.subscribe(this);
    }

    @Override
    public void detach() {
        eventBus.unsubscribe(this);
        super.detach();
    }


//    @EventBusListenerMethod
//    void onLogin(SuccessfulLoginEvent loginEvent) {
//        if (/* loginEvent.getSource().equals(this) */ true) {
//            access(new Runnable() {
//                @Override
//                public void run() {
//                    updateContent();
//                }
//            });
//        } else {
//            // We cannot inject the Main Screen if the event was fired from another UI, since that UI's scope would be
//            // active
//            // and the main screen for that UI would be injected. Instead, we just reload the page and let the init(...)
//            // method
//            // do the work for us.
//            getPage().reload();
//        }
//    }
}
