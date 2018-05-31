package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.progress.Registration;
import org.openthinclient.web.event.DashboardEvent.BrowserResizeEvent;
import org.openthinclient.web.event.DashboardEvent.CloseOpenWindowsEvent;
import org.openthinclient.web.event.DashboardEvent.UserLoggedOutEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.event.PackageManagerTaskActivatedEvent;
import org.openthinclient.web.ui.event.PackageManagerTaskFinalizedEvent;
import org.openthinclient.web.view.MainView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.security.VaadinSecurity;
import org.vaadin.spring.security.util.SuccessfulLoginEvent;
import org.vaadin.spring.sidebar.components.ValoSideBar;


@Theme("dashboard")
@Title("openthinclient.org")
@SpringUI
public final class ManagerUI extends UI {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 4314279050575370517L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerUI.class);

    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    VaadinSecurity vaadinSecurity;
    @Autowired
    SpringViewProvider viewProvider;
    @Autowired
    ValoSideBar sideBar;
    @Autowired
    PackageManagerExecutionEngine packageManagerExecutionEngine;
    @Autowired
    private EventBus.SessionEventBus eventBus;


  private Registration taskFinalizedRegistration;
  private Registration taskActivatedRegistration;

    protected void onPackageManagerTaskFinalized(ListenableProgressFuture<?> listenableProgressFuture) {
        eventBus.publish(this, new PackageManagerTaskFinalizedEvent(packageManagerExecutionEngine));
    }

    protected void onPackageManagerTaskActivated(ListenableProgressFuture<?> listenableProgressFuture) {
        eventBus.publish(this, new PackageManagerTaskActivatedEvent(packageManagerExecutionEngine));
    }

    @Override
    protected void init(final VaadinRequest request) {

        setLocale(LocaleUtil.getLocaleForMessages(ConsoleWebMessages.class, UI.getCurrent().getLocale()));

        Responsive.makeResponsive(this);
        addStyleName(ValoTheme.UI_WITH_MENU);

        // Some views need to be aware of browser resize events so a
        // BrowserResizeEvent gets fired to the event bus on every occasion.
        Page.getCurrent().addBrowserWindowResizeListener(event ->  eventBus.publish(this,(new BrowserResizeEvent(event.getHeight(), event.getWidth()))));

        IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
        Page.getCurrent().setTitle(mc.getMessage(ConsoleWebMessages.UI_PAGE_TITLE));

        taskActivatedRegistration = packageManagerExecutionEngine.addTaskActivatedHandler(this::onPackageManagerTaskActivated);
        taskFinalizedRegistration = packageManagerExecutionEngine.addTaskFinalizedHandler(this::onPackageManagerTaskFinalized);

        if (userHasAuthorities()) {
          showMainScreen();
        } else {
          showLoginScreen();
        }
    }


  private void showLoginScreen() {
    setContent(applicationContext.getBean(LoginView.class));
  }

  private void showMainScreen() {
    setContent(applicationContext.getBean(MainView.class));
  }

  @EventBusListenerMethod
  void onLogin(SuccessfulLoginEvent loginEvent) {

    if (loginEvent.getSource().equals(this)) {
      access(this::showMainScreen);
    } else {
      // We cannot inject the Main Screen if the event was fired from another UI, since that UI's scope would be
      // active
      // and the main screen for that UI would be injected. Instead, we just reload the page and let the init(...)
      // method
      // do the work for us.
      getPage().reload();
    }
  }

    private boolean userHasAuthorities() {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null) {
        Object principal = authentication.getPrincipal();
        boolean hasAuthorities = vaadinSecurity.hasAuthorities("ROLE_ADMINISTRATORS");
        return principal instanceof UserDetails && hasAuthorities;
      }
      return false;
    }

    @EventBusListenerMethod
    public void userLoggedOut(final UserLoggedOutEvent event) {

        LOGGER.debug("Received UserLoggedOutEvent for ", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        // When the user logs out, current VaadinSession gets closed and the
        // page gets reloaded on the login screen. Do notice the this doesn't
        // invalidate the current HttpSession.
        VaadinSession.getCurrent().close();
        SecurityContextHolder.getContext().setAuthentication(null);
        vaadinSecurity.logout();
        Page.getCurrent().reload();
    }

    @EventBusListenerMethod
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
        taskActivatedRegistration.unregister();
        taskFinalizedRegistration.unregister();
        eventBus.unsubscribe(this);
        super.detach();
    }
}
