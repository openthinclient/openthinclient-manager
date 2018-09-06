package org.openthinclient.web.dashboard;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import org.openthinclient.web.ui.OtcView;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name= DashboardView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.COMMON, caption = "Dashboard", order=1)
@ThemeIcon("icon/meter.svg")
public class DashboardView extends Panel implements View { // extends OtcView {

    public final static String NAME = "";

    final IMessageConveyor mc;
    private CssLayout dashboardPanels;

    @Autowired
    public DashboardView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
//        super(UI_DASHBOARDVIEW_HEADER, eventBus, notificationService);
        mc = new MessageConveyor(UI.getCurrent().getLocale());
        setContent(buildContent());
    }

    private Component buildContent() {
        dashboardPanels = new CssLayout();
        dashboardPanels.addStyleName("dashboard-panels");
        Responsive.makeResponsive(dashboardPanels);

        ContentPanel helpPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_TITLE), new ThemeResource("icon/help.svg"));
        helpPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_CONTENT), ContentMode.HTML));

        ContentPanel otcPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_OTC_TITLE), new ThemeResource("icon/logo.svg"));
        otcPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_OTC_CONTENT), ContentMode.HTML));

        ContentPanel toolsPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_TITLE), new ThemeResource("icon/meter.svg"));
        toolsPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_CONTENT), ContentMode.HTML));

        dashboardPanels.addComponents(helpPanel, otcPanel, toolsPanel);

        return dashboardPanels;
    }





}
