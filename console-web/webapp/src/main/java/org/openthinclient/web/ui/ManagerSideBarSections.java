package org.openthinclient.web.ui;

import org.vaadin.spring.sidebar.annotation.SideBarSection;
import org.vaadin.spring.sidebar.annotation.SideBarSections;

@SideBarSections({
        @SideBarSection(id = ManagerSideBarSections.DASHBOARD),
        @SideBarSection(id = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode = "UI_MENUSECTIONS_DEVICE_MANAGEMENT"),
        @SideBarSection(id = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_MENUSECTIONS_SERVER_MANAGEMENT")
})
public class ManagerSideBarSections {
  public static final String DASHBOARD = "dashboard";
  public static final String DEVICE_MANAGEMENT = "client-management";
  public static final String SERVER_MANAGEMENT = "server-management";
}
