package org.openthinclient.web.ui;

import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import org.openthinclient.web.OTCSideBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.vaadin.spring.sidebar.annotation.SideBarItem;


@SuppressWarnings("serial")
//@SpringView(name = SettingsView.NAME)
//@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode="UI_SETTINGS_HEADER", order = 99)
public final class SettingsView extends Window implements View {

  public static final String NAME = "settings_view";
  private static final Logger LOGGER = LoggerFactory.getLogger(SettingsView.class);


  private OTCSideBar settingsSideBar;

  private HorizontalLayout root;

  public SettingsView(OTCSideBar settingsSideBar) {

    setHeight("90%");
    setWidth("90%");
    center();
    setModal(true);
    setResizable(false);
//    setClosable(false);

    root = new HorizontalLayout();
    root.setSpacing(false);
    root.setSizeFull();
    settingsSideBar.setId("settingsmenu");
    root.addComponent(settingsSideBar);

    VerticalLayout vl = new VerticalLayout();
    vl.setSpacing(false);
    vl.setMargin(false);
    vl.setSizeFull();

    ComponentContainer content = new CssLayout();
    content.addStyleName("view-content");
    content.setSizeFull();
    vl.addComponent(content);
    vl.setExpandRatio(content, 1.0f);

//    final Navigator navigator = new Navigator(UI.getCurrent(), content);
//    navigator.addProvider(viewProvider);
//    if (navigator.getState().isEmpty()) {
//      navigator.navigateTo(DashboardView.NAME);
//    } else {
//      navigator.navigateTo(navigator.getState());
//    }

    root.addComponents(vl);
    root.setExpandRatio(vl, 1.0f);

    setContent(root);
  }


  public String getViewName() {
    return NAME;
  }



}
