package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import com.kstruct.gethostname4j.Hostname;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.Page;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringViewDisplay;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.common.model.service.UserService;
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.progress.Registration;
import org.openthinclient.service.common.license.LicenseChangeEvent;
import org.openthinclient.service.common.license.LicenseManager;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.component.LicenseMessageBar;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.event.DashboardEvent.BrowserResizeEvent;
import org.openthinclient.web.event.DashboardEvent.CloseOpenWindowsEvent;
import org.openthinclient.web.event.DashboardEvent.UserLoggedOutEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.ApplicationGroupView;
import org.openthinclient.web.thinclient.ApplicationView;
import org.openthinclient.web.thinclient.ClientView;
import org.openthinclient.web.thinclient.DeviceView;
import org.openthinclient.web.thinclient.HardwaretypeView;
import org.openthinclient.web.thinclient.LocationView;
import org.openthinclient.web.thinclient.PrinterView;
import org.openthinclient.web.thinclient.UserView;
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

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_SEARCH_NO_RESULT;

@Theme("openthinclient")
@SpringUI
@SpringViewDisplay
@Push(PushMode.MANUAL)
@com.vaadin.annotations.JavaScript({"vaadin://js/UIFunctions.js"})
public final class ManagerUI extends UI implements ViewDisplay, View {

  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = 4314279050575370517L;

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagerUI.class);

  public static final long REFRESH_DASHBOARD_MILLS = 10000;

  @Autowired
  ApplicationContext applicationContext;
  @Autowired
  VaadinSecurity vaadinSecurity;
  @Autowired
  SpringViewProvider viewProvider;
  @Autowired @Qualifier("deviceSideBar")
  OTCSideBar deviceSideBar;
  @Autowired
  PackageManagerExecutionEngine packageManagerExecutionEngine;
  @Autowired
  private EventBus.SessionEventBus eventBus;
  @Autowired
  SpringViewProvider springViewProvider;

  //
  @Autowired
  private RealmService realmService;
  @Autowired
  private PrinterService printerService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private DeviceService deviceService;
  @Autowired
  private HardwareTypeService hardwareTypeService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private UserService userService;
  @Autowired
  private LicenseManager licenseManager;


  private Registration taskFinalizedRegistration;
  private Registration taskActivatedRegistration;
  private Panel springViewDisplay;

  private IMessageConveyor mc;
  private AbstractOrderedLayout root;
  private Label titleLabel;
  private LicenseMessageBar licenseMessageBar;

  private UserProfileSubWindow userProfileWindow;
  private ComboBox<DirectoryObject> searchTextField;

  private boolean runThread = true;

  protected void onPackageManagerTaskFinalized(
      ListenableProgressFuture<?> listenableProgressFuture) {
    eventBus.publish(this, new PackageManagerTaskFinalizedEvent(packageManagerExecutionEngine));
  }

  protected void onPackageManagerTaskActivated(
      ListenableProgressFuture<?> listenableProgressFuture) {
    eventBus.publish(this, new PackageManagerTaskActivatedEvent(packageManagerExecutionEngine));
  }


  @PostConstruct
  public void init() {
    springViewProvider.setAccessDeniedViewClass(AccessDeniedView.class);
  }

  @Override
  public void showView(View view) {
    if (springViewDisplay != null) springViewDisplay.setContent((Component)view);
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

    Page.getCurrent().getStyles().add(".v-filterselect-suggestpopup-header-searchfield {--no-results-feedback: \""+ mc.getMessage(UI_COMMON_SEARCH_NO_RESULT) +"\"}");

    taskActivatedRegistration = packageManagerExecutionEngine.addTaskActivatedHandler(this::onPackageManagerTaskActivated);
    taskFinalizedRegistration = packageManagerExecutionEngine.addTaskFinalizedHandler(this::onPackageManagerTaskFinalized);

    searchTextField = new ComboBox<>();

    licenseMessageBar = new LicenseMessageBar(licenseManager, clientService);

    showMainScreen();

    JavaScript.getCurrent().execute("installGridTooltips()");
    JavaScript.getCurrent().execute("installInfoButtonFunction()");

    buildSearchTextField();
    createUserProfileWindow();

    addClickListener(e -> eventBus.publish(e, new CloseOpenWindowsEvent()));

    new RefreshDashboardThread().start();
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
    image.addClickListener(e -> UI.getCurrent().getPage().setLocation(""));
    image.addStyleName("logo-button");
    image.removeStyleName(ValoTheme.MENU_LOGO);
    deviceSideBar.setLogo(image);

    root = new HorizontalLayout();
    root.setSpacing(false);
    root.setSizeFull();
    deviceSideBar.setId("mainmenu");
    root.addComponent(deviceSideBar);

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
          searchTextField.setValue(null);
          JavaScript.getCurrent().execute("disableSpellcheck()");

          if(event.getNavigator().getState() != null) {
            deviceSideBar.updateFilterGrid(event.getNewView(), event.getParameters());
          }
        }
    });
    navigator.addProvider(viewProvider);
    navigator.navigateTo(navigator.getState());

    root.addComponents(vl);
    root.setExpandRatio(vl, 1.0f);

    setContent(root);
  }

  @EventBusListenerMethod
  public void userLoggedOut(final UserLoggedOutEvent event) {
    SecurityContext securityContext = (SecurityContext) VaadinSession.getCurrent().getSession().getAttribute("SPRING_SECURITY_CONTEXT");
    Authentication authentication = securityContext.getAuthentication();
    LOGGER.debug("Received UserLoggedOutEvent " + (authentication != null ? authentication.getPrincipal() : "null"));
    // When the user logs out, current VaadinSession gets closed and the
    // page gets reloaded on the login screen. Do notice the this doesn't
    // invalidate the current HttpSession.
    VaadinSession.getCurrent().close();
    vaadinSecurity.logout();
  }

  @EventBusListenerMethod
  public void closeOpenWindows(final CloseOpenWindowsEvent event) {
      for (Window window : UI.getCurrent().getWindows()) {
          window.close();
          UI.getCurrent().removeWindow(window);
      }
  }

  @EventBusListenerMethod
  public void updateHeaderLabel(final DashboardEvent.UpdateHeaderLabelEvent event) {
    if (titleLabel != null) {
      titleLabel.setValue(event.getCaption());
    }
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
      LOGGER.debug("Detach ManagerUI " + this + " and stop Thread");
      runThread = false;
      taskActivatedRegistration.unregister();
      taskFinalizedRegistration.unregister();
      eventBus.unsubscribe(this);
      super.detach();
  }

  private Component buildHeader() {
    CssLayout header = new CssLayout(
      getRealmLabel(),
      searchTextField,
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

  private void buildSearchTextField() {
    searchTextField.addStyleName("header-searchfield");
    searchTextField.setEmptySelectionAllowed(false);
    searchTextField.setPopupWidth("300px");
    searchTextField.addValueChangeListener(this::onSearchSelect);
    searchTextField.setItemCaptionGenerator(DirectoryObject::getName);
    searchTextField.setItemIconGenerator(profile -> {
      String icon;
      if (profile instanceof Application) {
        icon = ApplicationView.ICON;
      } else if (profile instanceof ApplicationGroup) {
        icon = ApplicationGroupView.ICON;
      } else if (profile instanceof Printer) {
        icon = PrinterView.ICON;
      } else if (profile instanceof HardwareType) {
        icon = HardwaretypeView.ICON;
      } else if (profile instanceof Device) {
        icon = DeviceView.ICON;
      } else if (profile instanceof ClientMetaData) {
        icon = ClientView.ICON;
      } else if (profile instanceof Client) {
        icon = ClientView.ICON;
      } else if (profile instanceof Location) {
        icon = LocationView.ICON;
      } else if (profile instanceof User) {
        icon = UserView.ICON;
      } else {
        return null;
      }
      return new ThemeResource(icon);
    });

    // TODO: perfom LDAP-search
    (new Thread() {
      @Override
      public void run() {
        long start = System.currentTimeMillis();
        List<DirectoryObject> directoryObjects = new ArrayList<>();
        try {
          directoryObjects.addAll(applicationService.findAll());
          directoryObjects.addAll(printerService.findAll());
          directoryObjects.addAll(deviceService.findAll());
          directoryObjects.addAll(hardwareTypeService.findAll());
          directoryObjects.addAll(locationService.findAll());
          directoryObjects.addAll(clientService.findAllClientMetaData());
          directoryObjects.addAll(userService.findAll());
          realmService.findAllRealms().forEach(realm ->
            directoryObjects.removeAll(realm.getAdministrators().getMembers())
          );
        } catch (Exception e) {
          LOGGER.warn("Cannot find clients for search: " + e.getMessage());
        }
        LOGGER.info("Setup directoryObjects-grid took " + (System.currentTimeMillis() - start) + "ms");
        eventBus.publish(this, new DashboardEvent.SearchObjectsSetupEvent(directoryObjects));
      }
    }).start();
  }

  @EventBusListenerMethod
  public void setupSearchObjects(DashboardEvent.SearchObjectsSetupEvent ev) {
    ListDataProvider<DirectoryObject> dataProvider = DataProvider.ofCollection(ev.getDirectoryObjects());
    dataProvider.setSortOrder(source -> source.getName().toLowerCase(), SortDirection.ASCENDING);
    searchTextField.setDataProvider(dataProvider.filteringBy(
      (directoryObject, filterText) -> {
        String value = filterText.toLowerCase();
        if(directoryObject.getName().toLowerCase().contains(value)) {
            return true;
        } else if (directoryObject instanceof ClientMetaData) {
          String macaddress = ((ClientMetaData) directoryObject).getMacAddress();
          if(macaddress != null && macaddress.contains(value)) {
            return true;
          }
        }
        return false;
      }
    ));
  }

  private void onSearchSelect(HasValue.ValueChangeEvent<DirectoryObject> event) {
    DirectoryObject directoryObject = event.getValue();
    String navigationState = null;
    if (directoryObject instanceof ApplicationGroup) {
      navigationState = ApplicationGroupView.NAME;
    } else if (directoryObject instanceof Application) {
      navigationState = ApplicationView.NAME;
    } else if (directoryObject instanceof ClientMetaData) {
      navigationState = ClientView.NAME;
    } else if (directoryObject instanceof Client) {
      navigationState = ClientView.NAME;
    } else if (directoryObject instanceof Device) {
      navigationState = DeviceView.NAME;
    } else if (directoryObject instanceof HardwareType) {
      navigationState = HardwaretypeView.NAME;
    } else if (directoryObject instanceof Location) {
      navigationState = LocationView.NAME;
    } else if (directoryObject instanceof Printer) {
      navigationState = PrinterView.NAME;
    } else if (directoryObject instanceof User) {
      navigationState = UserView.NAME;
    }

    if (navigationState != null) {
      getNavigator().navigateTo(navigationState + "/edit/" + directoryObject.getName());
    }
  }

  private Component buildLogoutButton() {

    UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    MenuBar menuBar = new MenuBar();
    menuBar.addStyleName(ValoTheme.MENUBAR_SMALL);
    menuBar.addStyleName("header-menu");

    final MenuBar.MenuItem file = menuBar.addItem(principal.getUsername(), null);
    file.addItem(mc.getMessage(ConsoleWebMessages.UI_PROFILE), this::showProfileSubWindow);
    file.addItem(mc.getMessage(ConsoleWebMessages.UI_LOGOUT), e -> eventBus.publish(this, new DashboardEvent.UserLoggedOutEvent()));

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


  private void createUserProfileWindow() {
    userProfileWindow = new UserProfileSubWindow(userService);
  }


  class RefreshDashboardThread extends Thread {

    @Override
    public void run() {
      LOGGER.info("Refreshing Dashboard each {} seconds.", (REFRESH_DASHBOARD_MILLS /1000));
      final ManagerUI ui = ManagerUI.this;
      try {
        // Update the data for a while
        while (runThread) {
          Thread.sleep(REFRESH_DASHBOARD_MILLS);
          if (ui.isAttached()) {
            try {
              ui.access(new Runnable() {
                @Override
                public void run() {
                  eventBus.publish(this, new DashboardEvent.PXEClientListRefreshEvent(null));
                }
              });
            } catch(com.vaadin.ui.UIDetachedException e){
              LOGGER.info("UIDetachedException detected, ui class=" + ui);
//            Authentication authentication = vaadinSecurity.getAuthentication();
//            LOGGER.error("UIDetachedException found when accessing ManagerUI with authentication="+authentication);
//            LOGGER.error("detached exception is "+e.getMessage());
            }
          } else {
            LOGGER.debug(ui + " not attached.");
          }
        }
      } catch (InterruptedException e) {
        LOGGER.error("Error while executing RefreshDashboardThread", e);
      }
    }
  }
}
