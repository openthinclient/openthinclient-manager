package org.openthinclient.web.view.dashboard;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
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
import org.openthinclient.web.ui.OtcViewPanel;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.ui.DashboardPanel;
import org.openthinclient.web.view.DashboardSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name= DashboardView.NAME)
@SideBarItem(sectionId = DashboardSections.COMMON, caption = "Dashboard", order=1)
public class DashboardView extends OtcViewPanel {

    public final static String NAME = "";

    final IMessageConveyor mc;
//    private final VerticalLayout root;
//    private final EventBus.SessionEventBus eventBus;
//    private final DashboardNotificationService notificationService;
//    private Label titleLabel;
//    private NotificationsButton notificationsButton;
    private CssLayout dashboardPanels;
//    private Window notificationsWindow;

    @Autowired
    public DashboardView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
        super("Dashboard", eventBus, notificationService);
        mc = new MessageConveyor(UI.getCurrent().getLocale());
    }

    @PostConstruct
    private void init() {
        setPanelContent(buildContent());
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





}
