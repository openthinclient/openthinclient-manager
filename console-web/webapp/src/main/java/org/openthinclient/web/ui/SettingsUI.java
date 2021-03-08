package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import com.kstruct.gethostname4j.Hostname;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.*;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.progress.Registration;
import org.openthinclient.service.common.license.LicenseChangeEvent;
import org.openthinclient.service.common.license.LicenseManager;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.component.LicenseMessageBar;
import org.openthinclient.web.event.DashboardEvent.BrowserResizeEvent;
import org.openthinclient.web.event.DashboardEvent.CloseOpenWindowsEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.event.PackageManagerTaskActivatedEvent;
import org.openthinclient.web.ui.event.PackageManagerTaskFinalizedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.security.VaadinSecurity;

import java.util.Locale;
import java.util.Optional;

@Theme("openthinclient")
@SpringUI(path = "/settings")
@Push(PushMode.MANUAL)
@com.vaadin.annotations.JavaScript({"vaadin://js/UIFunctions.js"})
public final class SettingsUI extends UI implements ViewDisplay {

  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = 4314279050575370517L;

  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsUI.class);

  @Autowired
  @Qualifier("settingsSideBar") OTCSideBar settingsSideBar;

  @Autowired
  ApplicationContext applicationContext;
  @Autowired
  VaadinSecurity vaadinSecurity;
  @Autowired
  SpringViewProvider viewProvider;
  @Autowired
  PackageManagerExecutionEngine packageManagerExecutionEngine;
  @Autowired
  private EventBus.SessionEventBus eventBus;
  @Autowired
  private RealmService realmService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private UserService userService;
  @Autowired
  private LicenseManager licenseManager;

  private Registration taskFinalizedRegistration;
  private Registration taskActivatedRegistration;
  private Panel springViewDisplay;

  private IMessageConveyor mc;
  private AbstractOrderedLayout root;
  private LicenseMessageBar licenseMessageBar;

  private UserProfileSubWindow userProfileWindow;

  protected void onPackageManagerTaskFinalized(
      ListenableProgressFuture<?> listenableProgressFuture) {
    eventBus.publish(this, new PackageManagerTaskFinalizedEvent(packageManagerExecutionEngine));
  }

  protected void onPackageManagerTaskActivated(
      ListenableProgressFuture<?> listenableProgressFuture) {
    eventBus.publish(this, new PackageManagerTaskActivatedEvent(packageManagerExecutionEngine));
  }

  @Override
  protected void init(final VaadinRequest request) {

    setLocale(LocaleUtil.getLocaleForMessages(ConsoleWebMessages.class, UI.getCurrent().getLocale()));
    Locale.setDefault(UI.getCurrent().getLocale()); // necessary for messages read from schemas

    addStyleName(ValoTheme.UI_WITH_MENU);

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    // Some views need to be aware of browser resize events so a
    // BrowserResizeEvent gets fired to the event bus on every occasion.
    Page.getCurrent().addBrowserWindowResizeListener(event -> eventBus.publish(this, (new BrowserResizeEvent(event.getHeight(), event.getWidth()))));

    Page.getCurrent().setTitle(mc.getMessage(ConsoleWebMessages.UI_PAGE_TITLE));

    taskActivatedRegistration = packageManagerExecutionEngine.addTaskActivatedHandler(this::onPackageManagerTaskActivated);
    taskFinalizedRegistration = packageManagerExecutionEngine.addTaskFinalizedHandler(this::onPackageManagerTaskFinalized);

    licenseMessageBar = new LicenseMessageBar(licenseManager, clientService);

    showMainScreen();

    JavaScript.getCurrent().execute("installGridTooltips()");
    JavaScript.getCurrent().execute("installInfoButtonFunction()");

    addClickListener(e -> eventBus.publish(e, new CloseOpenWindowsEvent()));

    userProfileWindow = new UserProfileSubWindow(userService);
  }


  /**
   *
   * |-------------|-----------------|
   * |   Logo      |     Header      |
   * |-------------|-----------------|
   * |   Section   |                 |
   * |-------------|    Content      |
   * |    Item     |                 |
   * |    Item     |                 |
   * |    Item     |                 |
   * |             |                 |
   * |-------------|-----------------|
   */
  private void showMainScreen() {

    Image image = new Image(null, new ThemeResource("logo.svg"));
    image.addClickListener(e -> UI.getCurrent().getPage().setLocation("/"));
    image.addStyleName("logo-button");
    image.removeStyleName(ValoTheme.MENU_LOGO);
    settingsSideBar.setLogo(image);

    root = new HorizontalLayout();
    root.setSpacing(false);
    root.setSizeFull();
    settingsSideBar.setId("mainmenu");
    root.addComponent(settingsSideBar);

    VerticalLayout vl = new VerticalLayout();
    vl.setSpacing(false);
    vl.setMargin(false);
    vl.setSizeFull();

    vl.addComponents(buildHeader(), licenseMessageBar);

    ComponentContainer content = new CssLayout();
    content.addStyleName("view-content");
    content.setSizeFull();
    vl.addComponent(content);
    vl.setExpandRatio(content, 1.0f);

    final Navigator navigator = new Navigator(UI.getCurrent(), content);
    navigator.addViewChangeListener(new ViewChangeListener() {
        @Override
        public boolean beforeViewChange(ViewChangeEvent event) {
          return true;
        }

        @Override
        public void afterViewChange(ViewChangeEvent event) {
          JavaScript.getCurrent().execute("disableSpellcheck()");
        }
    });
    navigator.addProvider(viewProvider);
    if (navigator.getState().isEmpty()) {
//      navigator.navigateTo(UpdateManagerView.NAME);
      navigator.navigateTo("realm_settings_view");
    } else {
      navigator.navigateTo(navigator.getState());
    }

    root.addComponents(vl);
    root.setExpandRatio(vl, 1.0f);

    setContent(root);
  }

  @EventBusListenerMethod
  public void licenseChange(LicenseChangeEvent ev) {
    if(licenseMessageBar != null) {
      licenseMessageBar.updateContent();
      this.push();
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

    private Component buildHeader() {
      CssLayout header = new CssLayout(
        getRealmLabel(),
        buildLogoutButton()
      );
      header.addStyleName("header");
      return header;
    }

    private Component getRealmLabel() {
      Optional<Realm> realm = realmService.findAllRealms().stream().findFirst();
      String description = realm.isPresent()? realm.get().getDescription() : "";
      String hostname = Hostname.getHostname();

      Layout realmLabel = new CssLayout();
      realmLabel.addStyleName("realm-label");
      realmLabel.addComponents(
        new Label(hostname),
        new Label(description)
      );

      return realmLabel;
    }

  private Component buildLogoutButton() {

    UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    MenuBar menuBar = new MenuBar();
    menuBar.addStyleName(ValoTheme.MENUBAR_SMALL);
    menuBar.addStyleName("header-menu");

    final MenuBar.MenuItem file = menuBar.addItem(principal.getUsername(), null);
    file.addItem(mc.getMessage(ConsoleWebMessages.UI_PROFILE), this::showProfileSubWindow);
    file.addItem(mc.getMessage(ConsoleWebMessages.UI_LOGOUT), e -> {
      // TODO: this is duplicate code
      SecurityContext securityContext = (SecurityContext) VaadinSession.getCurrent().getSession().getAttribute("SPRING_SECURITY_CONTEXT");
      Authentication authentication = securityContext.getAuthentication();
      LOGGER.debug("Received UserLoggedOutEvent " + (authentication != null ? authentication.getPrincipal() : "null"));
      // When the user logs out, current VaadinSession gets closed and the
      // page gets reloaded on the login screen. Do notice the this doesn't
      // invalidate the current HttpSession.
      VaadinSession.getCurrent().close();
      vaadinSecurity.logout();
    });

    return menuBar;
  }

  private void showProfileSubWindow(MenuBar.MenuItem menuItem) {

    if (!UI.getCurrent().getWindows().contains(userProfileWindow)) {
      SecurityContext securityContext = (SecurityContext) VaadinSession.getCurrent().getSession().getAttribute("SPRING_SECURITY_CONTEXT");
      Authentication authentication = securityContext.getAuthentication();
      UserDetails principal = (UserDetails) authentication.getPrincipal();
      try {
        userProfileWindow.refresh(userService.findByName(principal.getUsername()));
      } catch (Exception e) {
        LOGGER.warn("Cannot find directory-object: " + e.getMessage());
        userProfileWindow.showError(e);
      }
      UI.getCurrent().addWindow(userProfileWindow);
    } else {
      userProfileWindow.close();
      UI.getCurrent().removeWindow(userProfileWindow);
    }
  }

//  @Override
//  public void enter(ViewChangeListener.ViewChangeEvent event) {
//    LOGGER.debug("enter -> source={}, navigator-state=", event.getSource(), event.getNavigator().getState());
//    if (event.getParameters() != null) {
//      // split at "/", add each part as a label
//      String[] params = event.getParameters().split("/");
//    }
//  }
//
//  @Override
//  public boolean beforeViewChange(ViewChangeEvent event) {
//    return false;
//  }

  @Override
  public void showView(View view) {
    springViewDisplay.setContent((Component)view);
  }
}
