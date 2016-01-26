package org.openthinclient.web.view;

import org.vaadin.spring.sidebar.annotation.SideBarSection;
import org.vaadin.spring.sidebar.annotation.SideBarSections;

@SideBarSections({
        @SideBarSection(id = DashboardSections.COMMON, caption = "Common"),
        @SideBarSection(id = DashboardSections.PACKAGE_MANAGEMENT, caption = "Package Management")
})
public class DashboardSections {

  public static final String COMMON = "common";
  public static final String PACKAGE_MANAGEMENT = "package-management";

}
