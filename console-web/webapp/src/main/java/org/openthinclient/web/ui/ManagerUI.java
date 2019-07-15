package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.navigator.*;
import com.vaadin.server.*;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringViewDisplay;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.*;
import com.vaadin.ui.renderers.ImageRenderer;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.progress.Registration;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.dashboard.DashboardView;
import org.openthinclient.web.component.LicenseMessageBar;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.event.DashboardEvent.BrowserResizeEvent;
import org.openthinclient.web.event.DashboardEvent.CloseOpenWindowsEvent;
import org.openthinclient.web.event.DashboardEvent.UserLoggedOutEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.*;
import org.openthinclient.web.ui.event.PackageManagerTaskActivatedEvent;
import org.openthinclient.web.ui.event.PackageManagerTaskFinalizedEvent;
import org.openthinclient.service.common.license.LicenseChangeEvent;
import org.openthinclient.service.common.license.LicenseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.security.VaadinSecurity;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_COMMON_SEARCH_NO_RESULT;

@Theme("openthinclient")
@Title("openthinclient.org")
@SpringUI
@SpringViewDisplay
@Push(PushMode.MANUAL)
@com.vaadin.annotations.JavaScript({"UIFunctions.js"})
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

  private CssLayout dashboardPanels;
  private Window notificationsWindow;
  private ConsoleWebMessages i18nTitleKey;
  private IMessageConveyor mc;
  private AbstractOrderedLayout root;
  private Label titleLabel;
  private LicenseMessageBar licenseMessageBar;

  private Window searchResultWindow;
  private UserProfileSubWindow userProfileWindow;
  private Grid<DirectoryObject> resultObjectGrid;

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
    springViewDisplay.setContent((Component)view);
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

    createResultObjectGrid();
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
          JavaScript.getCurrent().execute("disableSpellcheck()");
        }
    });
    navigator.addProvider(viewProvider);
    if (navigator.getState().isEmpty()) {
      navigator.navigateTo(DashboardView.NAME);
    } else {
      navigator.navigateTo(navigator.getState());
    }

    root.addComponents(vl);
    root.setExpandRatio(vl, 1.0f);

    setContent(root);
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
    HorizontalLayout header = new HorizontalLayout();
    header.setMargin(false);
    header.addStyleName("header");

    Component searchTextField = buildSearchTextField();
    header.addComponent(searchTextField);
    header.setComponentAlignment(searchTextField, Alignment.MIDDLE_RIGHT);

    Component logout = buildLogoutButton();
    header.addComponent(logout);
    header.setComponentAlignment(logout, Alignment.MIDDLE_RIGHT);

    return header;
  }

  private TextField buildSearchTextField() {
    TextField searchTextField = new TextField();
    searchTextField.setPlaceholder("search");
    searchTextField.setIcon(new ThemeResource("icon/magnify.svg"));
    searchTextField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
    searchTextField.addStyleName("header-searchfield");
    searchTextField.addValueChangeListener(this::onFilterTextChange);
    return searchTextField;
  }

  private void createResultObjectGrid() {
    resultObjectGrid = new Grid<>();
    resultObjectGrid.addStyleNames("directoryObjectSelectionGrid");
    resultObjectGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
    resultObjectGrid.removeHeaderRow(0);
    resultObjectGrid.addItemClickListener(this::resultObjectClicked);
    resultObjectGrid.setStyleGenerator(directoryObject -> directoryObject.getClass().getSimpleName().toLowerCase()); // Style based on directoryObject class
    Grid.Column<DirectoryObject, ThemeResource> imageColumn = resultObjectGrid.addColumn(
        profile -> {
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
        },
        new ImageRenderer<>()
    );
    resultObjectGrid.addColumn(DirectoryObject::getName);

    searchResultWindow = new Window(null, resultObjectGrid);
    searchResultWindow.setClosable(false);
    searchResultWindow.setResizable(false);
    searchResultWindow.setDraggable(false);
    searchResultWindow.addStyleName("header-search-result");
    searchResultWindow.setWidthUndefined();

    // TODO: Perfomance
    // fill objectGrid
    long start = System.currentTimeMillis();
    List<DirectoryObject> directoryObjects = new ArrayList<>();
    try {
      directoryObjects.addAll(applicationService.findAll());
      directoryObjects.addAll(printerService.findAll());
      directoryObjects.addAll(deviceService.findAll());
      directoryObjects.addAll(hardwareTypeService.findAll());
      directoryObjects.addAll(locationService.findAll());
      directoryObjects.addAll(clientService.findAllClientMeta());
      directoryObjects.addAll(userService.findAll().stream().filter(user -> !user.getName().equals("administrator")).collect(Collectors.toSet()));
    } catch (Exception e) {
      LOGGER.warn("Cannot find clients for search: " + e.getMessage());
    }
    ListDataProvider dataProvider = DataProvider.ofCollection(directoryObjects);
    dataProvider.setSortOrder(source -> ((DirectoryObject) source).getName().toLowerCase(), SortDirection.ASCENDING);
    resultObjectGrid.setDataProvider(dataProvider);
    LOGGER.info("Setup directoryObjects-grid took " + (System.currentTimeMillis() - start) + "ms");

  }

  private void resultObjectClicked(Grid.ItemClick<DirectoryObject> directoryObjectItemClick) {

    // only take double-clicks
    if (directoryObjectItemClick.getMouseEventDetails().isDoubleClick()) {

      DirectoryObject directoryObject = directoryObjectItemClick.getItem();
      String navigationState = null;
      if (directoryObject instanceof ApplicationGroup) {
        navigationState = ApplicationGroupView.NAME;
      } else if (directoryObject instanceof Application) {
        navigationState = ApplicationView.NAME;
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
        UI.getCurrent().removeWindow(searchResultWindow);

        getNavigator().navigateTo(navigationState + "/" + directoryObject.getName());
      }
    }
  }

  private void onFilterTextChange(HasValue.ValueChangeEvent<String> event) {
    if (event.getValue().length() > 0) {
      ListDataProvider<DirectoryObject> dataProvider = (ListDataProvider<DirectoryObject>) resultObjectGrid.getDataProvider();
      dataProvider.setFilter(directoryObject ->
             caseInsensitiveContains(directoryObject.getName(), event.getValue()) ||
             clientSpecificParamContains(directoryObject, event.getValue())
      );

      // TODO: Resizing result- and window-height, improve this magic: references style .v-window-header-search-result max-height
      int resultSize = dataProvider.size(new Query<>());
      if (resultSize > 0) {
        searchResultWindow.setContent(resultObjectGrid);
        int windowHeight = (resultSize * 37);
        resultObjectGrid.setHeight(windowHeight > 300 ? 300 : windowHeight, Unit.PIXELS);
        searchResultWindow.setHeight(windowHeight > 300 ? 300 : windowHeight, Unit.PIXELS);
      } else {
        searchResultWindow.setContent(new Label(mc.getMessage(UI_COMMON_SEARCH_NO_RESULT)));
      }
      if (!UI.getCurrent().getWindows().contains(searchResultWindow)) {
        UI.getCurrent().addWindow(searchResultWindow);
      }
    } else {
      UI.getCurrent().removeWindow(searchResultWindow);
    }

  }

  private boolean clientSpecificParamContains(DirectoryObject directoryObject, String value) {
    if (directoryObject instanceof Client) {
      String macaddress = ((Client) directoryObject).getValue("macaddress");
      return macaddress != null && macaddress.contains(value.toLowerCase());
    }
    return false;
  }

  private Boolean caseInsensitiveContains(String where, String what) {
    return where.toLowerCase().contains(what.toLowerCase());
  }

  private Component buildLogoutButton() {

    HorizontalLayout hl = new HorizontalLayout();
    hl.setMargin(new MarginInfo(false, true, false, false));
    hl.setSpacing(false);

    UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    Label circle = new Label(principal.getUsername().substring(0,1).toUpperCase());
    circle.addStyleName("header-circle");
    hl.addComponent(circle);

    MenuBar menuBar = new MenuBar();
    menuBar.setWidth("100%");
    menuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
    menuBar.addStyleName(ValoTheme.MENUBAR_SMALL);
    menuBar.addStyleName("header-menu");

    hl.addComponent(menuBar);

    final MenuBar.MenuItem file = menuBar.addItem(principal.getUsername(), null);
    file.addItem(mc.getMessage(ConsoleWebMessages.UI_PROFILE), this::showProfileSubWindow);
    file.addItem(mc.getMessage(ConsoleWebMessages.UI_LOGOUT), e -> eventBus.publish(this, new DashboardEvent.UserLoggedOutEvent()));

    return hl;
  }

  private void showProfileSubWindow(MenuBar.MenuItem menuItem) {

    if (!UI.getCurrent().getWindows().contains(userProfileWindow)) {
      UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
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

  private void toggleMaximized(final Component panel, final boolean maximized) {
    for (Iterator<Component> it = root.iterator(); it.hasNext();) {
      it.next().setVisible(!maximized);
    }
    dashboardPanels.setVisible(true);

    for (Iterator<Component> it = dashboardPanels.iterator(); it.hasNext();) {
      Component c = it.next();
      c.setVisible(!maximized);
    }

    if (maximized) {
      panel.setVisible(true);
      panel.addStyleName("max");
    } else {
      panel.removeStyleName("max");
    }
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
