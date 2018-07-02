package org.openthinclient.web.view.dashboard;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.google.common.eventbus.Subscribe;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import java.util.Collection;
import java.util.Iterator;
import org.openthinclient.web.domain.DashboardNotification;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.event.DashboardEvent.CloseOpenWindowsEvent;
import org.openthinclient.web.event.DashboardEvent.NotificationsCountUpdatedEvent;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.ui.DashboardPanel;
import org.openthinclient.web.view.DashboardSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name= DashboardView.NAME)
@SideBarItem(sectionId = DashboardSections.COMMON, caption = "Dashboard", order=1)
public class DashboardView extends Panel implements View {

    public final static String NAME = "";

    final IMessageConveyor mc;
    private final VerticalLayout root;
    private final EventBus.SessionEventBus eventBus;
    private final DashboardNotificationService notificationService;
    private Label titleLabel;
    private NotificationsButton notificationsButton;
    private CssLayout dashboardPanels;
    private Window notificationsWindow;

    @Autowired
    public DashboardView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
        this.eventBus = eventBus;
        this.notificationService = notificationService;

        mc = new MessageConveyor(UI.getCurrent().getLocale());

        addStyleName(ValoTheme.PANEL_BORDERLESS);
        setSizeFull();
        DashboardEventBus.register(this);

        root = new VerticalLayout();
        root.setSizeFull();
        root.setMargin(true);
        root.addStyleName("dashboard-view");
        setContent(root);
        Responsive.makeResponsive(root);

        root.addComponent(buildHeader());

        Component content = buildContent();
        root.addComponent(content);
        root.setExpandRatio(content, 1);

        // All the open sub-windows should be closed whenever the root layout
        // gets clicked.
        root.addLayoutClickListener(new LayoutClickListener() {
            @Override
            public void layoutClick(final LayoutClickEvent event) {
                DashboardEventBus.post(new CloseOpenWindowsEvent());
            }
        });
    }

    private Component buildHeader() {

        final ViewHeader header = new ViewHeader("Dashboard");

        notificationsButton = buildNotificationsButton();
        LogoutButton logout = buildLogoutButton();
        header.addTools(notificationsButton, /* edit, */ logout);

        return header;
    }

    private NotificationsButton buildNotificationsButton() {
        NotificationsButton result = new NotificationsButton(notificationService);
        result.addClickListener((ClickListener) this::openNotificationsPopup);
        return result;
    }

    private LogoutButton buildLogoutButton() {
       LogoutButton result = new LogoutButton();
       result.addClickListener(new ClickListener() {
           @Override
           public void buttonClick(final ClickEvent event) {
              eventBus.publish(this, new DashboardEvent.UserLoggedOutEvent());
           }
       });
       return result;
   }

    private Component buildContent() {
        dashboardPanels = new CssLayout();
        dashboardPanels.addStyleName("dashboard-panels");
        Responsive.makeResponsive(dashboardPanels);

        DashboardPanel helpPanel = new DashboardPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_TITLE), new ThemeResource("icon/help.svg"));
        helpPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_CONTENT), ContentMode.HTML));

        DashboardPanel otcPanel = new DashboardPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_OTC_TITLE), new ThemeResource("icon/logo.svg"));
        otcPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_OTC_CONTENT), ContentMode.HTML));

        DashboardPanel toolsPanel = new DashboardPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_TITLE), new ThemeResource("icon/meter.svg"));
        toolsPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_CONTENT), ContentMode.HTML));

        dashboardPanels.addComponents(helpPanel, otcPanel, toolsPanel);

        return dashboardPanels;
    }

    private void openNotificationsPopup(final ClickEvent event) {
        VerticalLayout notificationsLayout = new VerticalLayout();
        notificationsLayout.setMargin(true);
        notificationsLayout.setSpacing(true);

        Label title = new Label(mc.getMessage(UI_DASHBOARDVIEW_NOTIFOCATIONS_CAPTION));
        title.addStyleName(ValoTheme.LABEL_H3);
        title.addStyleName(ValoTheme.LABEL_NO_MARGIN);
        notificationsLayout.addComponent(title);

        Collection<DashboardNotification> notifications = notificationService.getNotifications();
        DashboardEventBus.post(new NotificationsCountUpdatedEvent());

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
                new ClickListener() {
                    @Override
                    public void buttonClick(final ClickEvent event) {
                        Notification.show(mc.getMessage(UI_DASHBOARDVIEW_NOT_IMPLEMENTED));
                    }
                });
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
            notificationsWindow.setCloseShortcut(KeyCode.ESCAPE, null);
            notificationsWindow.setContent(notificationsLayout);
        }

        if (!notificationsWindow.isAttached()) {
            notificationsWindow.setPositionY(event.getClientY()
                    - event.getRelativeY() + 40);
            getUI().addWindow(notificationsWindow);
            notificationsWindow.focus();
        } else {
            notificationsWindow.close();
        }
    }

    @Override
    public void enter(final ViewChangeEvent event) {
        notificationsButton.updateNotificationsCount(null);
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

    public static final class NotificationsButton extends Button {
        public static final String ID = "dashboard-notifications";
        private static final String STYLE_UNREAD = "unread";
        private final DashboardNotificationService notificationService;

        public NotificationsButton(DashboardNotificationService notificationService) {
            this.notificationService = notificationService;
            setIcon(FontAwesome.BELL);
            setId(ID);
            addStyleName("notifications");
            addStyleName(ValoTheme.BUTTON_ICON_ONLY);
            DashboardEventBus.register(this);
        }

        @Subscribe
        public void updateNotificationsCount(
                final NotificationsCountUpdatedEvent event) {
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

       public static final String ID = "dashboard-logout";

       public LogoutButton() {
           setIcon(FontAwesome.SIGN_OUT);
           setId(ID);
           addStyleName("logout");
           addStyleName(ValoTheme.BUTTON_ICON_ONLY);
           DashboardEventBus.register(this);
       }

   }


}
