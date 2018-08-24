package org.openthinclient.web.ui;


import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_DASHBOARDVIEW_NOTIFOCATIONS_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_DASHBOARDVIEW_NOTIFOCATIONS_VIEWALL;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_DASHBOARDVIEW_NOT_IMPLEMENTED;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.event.LayoutEvents;
import com.vaadin.event.ShortcutAction;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import java.util.Collection;
import java.util.Iterator;
import org.openthinclient.web.domain.DashboardNotification;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

public class OtcView extends Panel implements View {

  private static final Logger LOGGER = LoggerFactory.getLogger(OtcView.class);

  private final IMessageConveyor mc;
  private final VerticalLayout root;
  private final EventBus.SessionEventBus eventBus;
  private final DashboardNotificationService notificationService;
  private NotificationsButton notificationsButton;
  private Component logout;
  private CssLayout dashboardPanels;
  private Window notificationsWindow;
  private ConsoleWebMessages i18nTitleKey;
  
  public OtcView(ConsoleWebMessages i18nTitleKey, EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
    
    this.eventBus = eventBus;
    this.notificationService = notificationService;
    this.i18nTitleKey = i18nTitleKey;

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    addStyleName(ValoTheme.PANEL_BORDERLESS);
    setSizeFull();

    root = new VerticalLayout();
    root.setSpacing(false);
    root.setSizeFull();
    root.setMargin(false);
    root.addStyleName("view");
    setContent(root);
    Responsive.makeResponsive(root);

    // the header
    root.addComponent(buildHeader());

    // All the open sub-windows should be closed whenever the root layout
    // gets clicked.
    root.addLayoutClickListener(new LayoutEvents.LayoutClickListener() {
      @Override
      public void layoutClick(final LayoutEvents.LayoutClickEvent event) {
        eventBus.publish(this, new DashboardEvent.CloseOpenWindowsEvent());
      }
    });


    // Configure the error handler for the UI
    UI.getCurrent().setErrorHandler(new DefaultErrorHandler() {
      @Override
      public void error(com.vaadin.server.ErrorEvent event) {
        LOGGER.error("Caught unexpected error.", event.getThrowable());
        // Display the error message in a custom fashion
//        Label errorMessage = new Label(mc.getMessage(ConsoleWebMessages.UI_UNEXPECTED_ERROR), ContentMode.HTML);
//        errorMessage.addStyleName("unexpected_error");
        Notification.show(mc.getMessage(ConsoleWebMessages.UI_UNEXPECTED_ERROR), Notification.Type.ERROR_MESSAGE);
      }
    });
  }

  private Component buildHeader() {

    VerticalLayout header = new VerticalLayout();
    header.setMargin(false);
    header.setStyleName("header");

    HorizontalLayout top = new HorizontalLayout();
    top.setStyleName("header-top");
    top.setSizeFull();

    TextField searchTextField = new TextField();
    searchTextField.setStyleName("header-searchfield");
    searchTextField.setPlaceholder("Search");
    searchTextField.setIcon(VaadinIcons.SEARCH);
    searchTextField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);

    top.addComponent(notificationsButton = buildNotificationsButton());
    top.addComponent(searchTextField);
    top.setComponentAlignment(searchTextField, Alignment.MIDDLE_LEFT);
    top.addComponent(logout = buildLogoutButton());
    top.setComponentAlignment(logout, Alignment.MIDDLE_RIGHT);

    HorizontalLayout bottom = new HorizontalLayout();
    bottom.setStyleName("header-bottom");
    bottom.setSizeFull();
    Label titleLabel = new Label(mc.getMessage(i18nTitleKey));
    titleLabel.setStyleName("header-title");
    bottom.addComponent(titleLabel);

    header.addComponents(top, bottom);
    return header;
  }

  public void setPanelContent(Component content) {
    root.addComponent(content);
    root.setExpandRatio(content, 1);
  }
  
  private NotificationsButton buildNotificationsButton() {
    NotificationsButton result = new NotificationsButton(notificationService);
    result.addClickListener((Button.ClickListener) this::openNotificationsPopup);
    return result;
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
    file.addItem("Profile", null);
    file.addItem("Logout", e -> eventBus.publish(this, new DashboardEvent.UserLoggedOutEvent()));

    return hl;
  }


  private void openNotificationsPopup(final Button.ClickEvent event) {
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

    if (notificationsWindow == null) {
      notificationsWindow = new Window();
      notificationsWindow.setWidth(300.0f, Unit.PIXELS);
      notificationsWindow.addStyleName("notifications");
      notificationsWindow.setClosable(false);
      notificationsWindow.setResizable(false);
      notificationsWindow.setDraggable(false);
      notificationsWindow.setCloseShortcut(ShortcutAction.KeyCode.ESCAPE, null);
      notificationsWindow.setContent(notificationsLayout);
    }

    if (!notificationsWindow.isAttached()) {
      notificationsWindow.setPositionY(event.getClientY() - event.getRelativeY() );
      getUI().addWindow(notificationsWindow);
      notificationsWindow.focus();
    } else {
      notificationsWindow.close();
    }
  }

  @Override
  public void enter(final ViewChangeListener.ViewChangeEvent event) {
    notificationsButton.updateNotificationsCount(new DashboardEvent.NotificationsCountUpdatedEvent());
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

  public class NotificationsButton extends Button {
//    public static final String ID = "dashboard-notifications";
    private static final String STYLE_UNREAD = "unread";
    private final DashboardNotificationService notificationService;

    public NotificationsButton(DashboardNotificationService notificationService) {
      this.notificationService = notificationService;
      setIcon(VaadinIcons.BELL);
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

  public static final class LogoutButton extends Button {

//    public static final String ID = "dashboard-logout";

    public LogoutButton() {
//      setIcon(FontAwesome.SIGN_OUT);
//      setId(ID);
//      addStyleName("logout");
//      addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      addStyleName("header-logout-button");
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
  
}
