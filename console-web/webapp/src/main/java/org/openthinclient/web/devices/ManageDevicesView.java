package org.openthinclient.web.devices;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_HEADER;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_DEVICEMANAGEMENT_HEADER;

import javax.annotation.PostConstruct;

import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.Sparklines;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import ch.qos.cal10n.MessageConveyor;

@SpringView(name = "devices")
@SideBarItem(sectionId = DashboardSections.DEVICE_MANAGEMENT, captionCode = "UI_DEVICEMANAGEMENT_HEADER", order = -100)
public class ManageDevicesView extends Panel implements View {

  /** serialVersionUID */
  private static final long serialVersionUID = -8836200902351197949L;

  final MessageConveyor mc;
  final VerticalLayout root ;
  
  public ManageDevicesView() {

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

     root.addComponent(new ViewHeader(mc.getMessage(UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_HEADER)));
     root.addComponent(new Sparklines());     
  }

  @Override
  public String getCaption() {
     return mc.getMessage(UI_DEVICEMANAGEMENT_HEADER);
  }

  @PostConstruct
  private void init() {
     Component content = buildContent();
     root.addComponent(content);
     root.setExpandRatio(content, 1);
  }
  
  private Component buildContent() {
     
     HorizontalLayout content = new HorizontalLayout();
     
     Label labelDescription = new Label(mc.getMessage(ConsoleWebMessages.UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_DESCRIPTION), ContentMode.HTML);
     labelDescription.setStyleName("devicemanagement-description");
     
     VerticalLayout leftPane = new VerticalLayout();
     leftPane.setStyleName("devicemanagement-leftpane");
     
     Image image = new Image("", new ThemeResource("./img/screenshot-manager.png"));
     image.setWidth(400, Unit.PIXELS);
     leftPane.addComponent(image);
     
     Link linkOpen = new Link();
     linkOpen.setCaption(mc.getMessage(ConsoleWebMessages.UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_LINK));
     linkOpen.setResource(new ExternalResource("/console/launch.jnlp"));
     leftPane.addComponent(linkOpen);
     
     content.addComponent(leftPane);
     content.addComponent(labelDescription);
     
     return content;
  }
  
  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

  }
}
