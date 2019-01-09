package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.data.HasValue;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.event.ShortcutAction;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.server.*;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.SpringViewDisplay;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.*;
import com.vaadin.ui.renderers.ComponentRenderer;
import com.vaadin.ui.renderers.HtmlRenderer;
import com.vaadin.ui.renderers.ImageRenderer;
import com.vaadin.ui.themes.ValoTheme;
import javax.annotation.PostConstruct;

import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.progress.Registration;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.domain.DashboardNotification;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.event.DashboardEvent.BrowserResizeEvent;
import org.openthinclient.web.event.DashboardEvent.CloseOpenWindowsEvent;
import org.openthinclient.web.event.DashboardEvent.UserLoggedOutEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.*;
import org.openthinclient.web.ui.event.PackageManagerTaskActivatedEvent;
import org.openthinclient.web.ui.event.PackageManagerTaskFinalizedEvent;
import org.openthinclient.web.dashboard.DashboardView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.security.VaadinSecurity;
import org.vaadin.spring.sidebar.components.ValoSideBar;

import java.util.*;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_DASHBOARDVIEW_NOTIFOCATIONS_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_DASHBOARDVIEW_NOTIFOCATIONS_VIEWALL;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_DASHBOARDVIEW_NOT_IMPLEMENTED;

@Theme("openthinclient")
@Title("openthinclient.org")
@SpringUI
@SpringViewDisplay
public final class ManagerUI extends UI implements ViewDisplay {

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

  private Registration taskFinalizedRegistration;
  private Registration taskActivatedRegistration;
  private Panel springViewDisplay;

  @Autowired
  private DashboardNotificationService notificationService;
  private NotificationsButton notificationsButton;
  private CssLayout dashboardPanels;
  private Window notificationsWindow;
  private ConsoleWebMessages i18nTitleKey;
  private IMessageConveyor mc;
  private AbstractOrderedLayout root;
  private Label titleLabel;

  private Window searchResultWindow;
  private Grid<DirectoryObject> resultObjectGrid;

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

    Responsive.makeResponsive(this);
    addStyleName(ValoTheme.UI_WITH_MENU);

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    // Some views need to be aware of browser resize events so a
    // BrowserResizeEvent gets fired to the event bus on every occasion.
    Page.getCurrent().addBrowserWindowResizeListener(event -> eventBus.publish(this, (new BrowserResizeEvent(event.getHeight(), event.getWidth()))));

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
    Page.getCurrent().setTitle(mc.getMessage(ConsoleWebMessages.UI_PAGE_TITLE));

    taskActivatedRegistration = packageManagerExecutionEngine.addTaskActivatedHandler(this::onPackageManagerTaskActivated);
    taskFinalizedRegistration = packageManagerExecutionEngine.addTaskFinalizedHandler(this::onPackageManagerTaskFinalized);

    createResultObjectGrid();
    createNotificationWindow();

    showMainScreen();

    addClickListener(e -> eventBus.publish(e, new CloseOpenWindowsEvent()));
  }

  private void showMainScreen() {

    root = new HorizontalLayout();
    root.setSpacing(false);
    root.setSizeFull();
    sideBar.setId("mainmenu");
    root.addComponent(sideBar);

    VerticalLayout vl = new VerticalLayout();
    vl.setSpacing(false);
    vl.setMargin(false);
    vl.setSizeFull();

    vl.addComponents(buildHeader());

    ComponentContainer content = new CssLayout();
    content.addStyleName("view-content");
    content.setSizeFull();
    vl.addComponent(content);
    vl.setExpandRatio(content, 1.0f);

    final Navigator navigator = new Navigator(UI.getCurrent(), content);
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
    VerticalLayout header = new VerticalLayout();
    header.setMargin(false);
    header.addStyleName("header");

    HorizontalLayout headerTop = new HorizontalLayout();
    headerTop.setMargin(false);
    headerTop.addStyleName("header-top");
    headerTop.setWidth("100%");

    headerTop.addComponent(notificationsButton = buildNotificationsButton());

    Component searchTextField = buildSearchTextField();
    headerTop.addComponent(searchTextField);
    headerTop.setComponentAlignment(searchTextField, Alignment.MIDDLE_LEFT);

    Component logout = buildLogoutButton();
    headerTop.addComponent(logout);
    headerTop.setComponentAlignment(logout, Alignment.MIDDLE_RIGHT);

    header.addComponent(headerTop);

    titleLabel = new Label();
    titleLabel.setStyleName("header-title");
    header.addComponent(titleLabel);

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
          ThemeResource resource;
          if (profile instanceof Application) {
            resource = ThinclientView.PACKAGES;
          } else if (profile instanceof ApplicationGroup) {
            resource =  ThinclientView.APPLICATIONGROUP;
          } else if (profile instanceof Printer) {
            resource =  ThinclientView.PRINTER;
          } else if (profile instanceof HardwareType) {
            resource =  ThinclientView.HARDWARE;
          } else if (profile instanceof Device) {
            resource =  ThinclientView.DEVICE;
          } else if (profile instanceof Client) {
            resource =  ThinclientView.CLIENT;
          } else if (profile instanceof Location) {
            resource =  ThinclientView.LOCATION;
          } else if (profile instanceof User) {
            resource =  ThinclientView.USER;
          } else {
            resource =  null;
          }
          return resource;
        },
        new ImageRenderer<>());
    resultObjectGrid.addColumn(DirectoryObject::getName);

    searchResultWindow = new Window(null, resultObjectGrid);
    searchResultWindow.setClosable(false);
    searchResultWindow.addStyleName("header-search-result");

    // fill objectGrid
    long start = System.currentTimeMillis();
    List<DirectoryObject> directoryObjects = new ArrayList<>();
    directoryObjects.addAll(applicationService.findAll());
    directoryObjects.addAll(printerService.findAll());
    directoryObjects.addAll(deviceService.findAll());
    directoryObjects.addAll(hardwareTypeService.findAll());
    directoryObjects.addAll(clientService.findAll());
    directoryObjects.addAll(locationService.findAll());
    ListDataProvider dataProvider = DataProvider.ofCollection(directoryObjects);
    dataProvider.setSortOrder(source -> ((DirectoryObject) source).getName(), SortDirection.ASCENDING);
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
      dataProvider.setFilter(DirectoryObject::getName, s -> caseInsensitiveContains(s, event.getValue()));
      // TODO: Resizing result- and window-height, improve this magic: references style .v-window-header-search-result max-height
      int windowHeight = (dataProvider.size(new Query<>()) * 37);
      resultObjectGrid.setHeight(windowHeight > 300 ? 300 : windowHeight, Unit.PIXELS);
      searchResultWindow.setHeight(windowHeight > 300 ? 300 : windowHeight, Unit.PIXELS);
      if (!UI.getCurrent().getWindows().contains(searchResultWindow)) {
        UI.getCurrent().addWindow(searchResultWindow);
      }
    } else {
      UI.getCurrent().removeWindow(searchResultWindow);
    }

  }

  private Boolean caseInsensitiveContains(String where, String what) {
    return where.toLowerCase().contains(what.toLowerCase());
  }

  private NotificationsButton buildNotificationsButton() {
    NotificationsButton result = new NotificationsButton(notificationService);
    result.addClickListener((Button.ClickListener) this::openNotificationsPopup);
    return result;
  }

  private void createNotificationWindow() {
    VerticalLayout notificationsLayout = new VerticalLayout();
    notificationsLayout.setMargin(true);
    notificationsLayout.setSpacing(true);

    Label title = new Label(mc.getMessage(UI_DASHBOARDVIEW_NOTIFOCATIONS_CAPTION));
    title.addStyleName(ValoTheme.LABEL_H3);
    title.addStyleName(ValoTheme.LABEL_NO_MARGIN);
    notificationsLayout.addComponent(title);

    Collection<DashboardNotification> notifications = notificationService.getNotifications();
    eventBus.publish(this, new DashboardEvent.NotificationsCountUpdatedEvent());

    for (DashboardNotification notification : notifications) {
      VerticalLayout notificationLayout = new VerticalLayout();
      notificationLayout.addStyleName("notification-item");

      Label titleLabel = new Label(notification.getFirstName() + " "
          + notification.getLastName() + " "
          + notification.getAction());
      titleLabel.addStyleName("notification-title");

      Label timeLabel = new Label(notification.getPrettyTime());
      timeLabel.addStyleName("notification-time");

      Label contentLabel = new Label(notification.getContent());
      contentLabel.addStyleName("notification-content");

      notificationLayout.addComponents(titleLabel, timeLabel,
          contentLabel);
      notificationsLayout.addComponent(notificationLayout);
    }

    HorizontalLayout footer = new HorizontalLayout();
    footer.addStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
    footer.setWidth("100%");
    Button showAll = new Button(mc.getMessage(UI_DASHBOARDVIEW_NOTIFOCATIONS_VIEWALL),
        e -> Notification.show(mc.getMessage(UI_DASHBOARDVIEW_NOT_IMPLEMENTED))
    );
    showAll.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    showAll.addStyleName(ValoTheme.BUTTON_SMALL);
    footer.addComponent(showAll);
    footer.setComponentAlignment(showAll, Alignment.TOP_CENTER);
    notificationsLayout.addComponent(footer);

    notificationsWindow = new Window();
    notificationsWindow.setWidth(300.0f, Unit.PIXELS);
    notificationsWindow.addStyleName("notifications");
    notificationsWindow.setClosable(false);
    notificationsWindow.setResizable(false);
    notificationsWindow.setDraggable(false);
    notificationsWindow.addCloseShortcut(ShortcutAction.KeyCode.ESCAPE, null);
    notificationsWindow.setContent(notificationsLayout);

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
    file.addItem(mc.getMessage(ConsoleWebMessages.UI_PROFILE), null);
    file.addItem(mc.getMessage(ConsoleWebMessages.UI_LOGOUT), e -> eventBus.publish(this, new DashboardEvent.UserLoggedOutEvent()));

    return hl;
  }


  private void openNotificationsPopup(final Button.ClickEvent event) {

    if (!UI.getCurrent().getWindows().contains(notificationsWindow)) {
      UI.getCurrent().addWindow(notificationsWindow);
      notificationsWindow.setPositionY(event.getClientY() - event.getRelativeY() );
//      notificationsWindow.focus();
    } else {
      notificationsWindow.close();
      UI.getCurrent().removeWindow(notificationsWindow);
    }
  }

//  @Override
//  public void enter(final ViewChangeListener.ViewChangeEvent event) {
//    notificationsButton.updateNotificationsCount(new DashboardEvent.NotificationsCountUpdatedEvent());
//  }

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

  public class NotificationsButton extends Button {
    //    public static final String ID = "dashboard-notifications";
    private static final String STYLE_UNREAD = "unread";
    private final DashboardNotificationService notificationService;

    public NotificationsButton(DashboardNotificationService notificationService) {
      this.notificationService = notificationService;
      setIcon(new ThemeResource("icon/bell.svg"));
//      setId(ID);
      addStyleName("header-notification-button");
      addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    }

    @EventBusListenerMethod
    public void updateNotificationsCount(final DashboardEvent.NotificationsCountUpdatedEvent event) {
      setUnreadCount(notificationService.getUnreadNotificationsCount());
    }

    public void setUnreadCount(final int count) {
      setCaption(String.valueOf(count));

      String description = "Notifications";
      if (count > 0) {
        addStyleName(STYLE_UNREAD);
        description += " (" + count + " unread)";
      } else {
        removeStyleName(STYLE_UNREAD);
      }
      setDescription(description);

      // only show the button if there are unread notifications
      setVisible(count != 0);
    }
  }


}
