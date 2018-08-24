package org.openthinclient.web.ui;

import org.vaadin.spring.sidebar.annotation.SideBarSection;
import org.vaadin.spring.sidebar.annotation.SideBarSections;

@SideBarSections({
        @SideBarSection(id = ManagerSideBarSections.COMMON, captionCode = "UI_DASHBOARDSECTIONS_COMMON"),
        @SideBarSection(id = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode = "UI_DASHBOARDSECTIONS_DEVICE_MANAGEMENT"),
        @SideBarSection(id = ManagerSideBarSections.PACKAGE_MANAGEMENT, captionCode = "UI_DASHBOARDSECTIONS_PACKAGE_MANAGEMENT"),
        @SideBarSection(id = ManagerSideBarSections.SERVICE_MANAGEMENT, captionCode = "UI_DASHBOARDSECTIONS_SERVICE_MANAGEMENT"),
        @SideBarSection(id = ManagerSideBarSections.SUPPORT, captionCode = "UI_DASHBOARDSECTIONS_SUPPORT")
})
public class ManagerSideBarSections {

  public static final String COMMON = "common";
  public static final String PACKAGE_MANAGEMENT = "package-management";
  public static final String DEVICE_MANAGEMENT  = "device-management";
  public static final String SERVICE_MANAGEMENT = "service-management";
  public static final String SUPPORT = "support";
}
