package org.openthinclient.web.view.dashboard;

import com.vaadin.navigator.View;
import com.vaadin.server.Responsive;

import org.openthinclient.web.dashboard.ui.design.DashboardDesign;

//@SuppressWarnings("serial")
//@SpringView(name= "dashboard")
//@SideBarItem(sectionId = DashboardSections.COMMON, caption = "Dashboard", order=1)
public class NewDashboardView extends DashboardDesign implements View{

  public NewDashboardView() {

    Responsive.makeResponsive( //
            this //
    );

    setSizeFull();
  }
}
