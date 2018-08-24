package org.openthinclient.web.component;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

/**
 *
 */
@SpringComponent
@SideBarItem(sectionId = ManagerSideBarSections.SUPPORT, caption = "Logout", order = 99)
@ViewScope
public class LogoutButton extends Button implements Runnable {

  @Autowired
  private EventBus.SessionEventBus eventBus;

  public LogoutButton() {
    super("Logout");
    super.addStyleName("sidebar-logout");
  }

  @Override
  public void run() {
    eventBus.publish(this, new DashboardEvent.UserLoggedOutEvent());
  }
}
