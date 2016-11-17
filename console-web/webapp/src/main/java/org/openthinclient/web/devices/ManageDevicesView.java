package org.openthinclient.web.devices;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;

import org.openthinclient.web.devices.ui.design.ManageDevicesDesign;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.view.DashboardSections;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import ch.qos.cal10n.MessageConveyor;

@SpringView(name = "devices")
@SideBarItem(sectionId = DashboardSections.DEVICE_MANAGEMENT, captionCode = "UI_DEVICEMANAGEMENT_HEADER", order = -100)
public class ManageDevicesView extends ManageDevicesDesign implements View {

  public ManageDevicesView() {
    final MessageConveyor conveyor = new MessageConveyor(UI.getCurrent().getLocale());

    labelTitle.setValue(conveyor.getMessage(ConsoleWebMessages.UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_HEADER));
    labelDescription.setValue(conveyor.getMessage(ConsoleWebMessages.UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_DESCRIPTION));
    linkOpen.setCaption(conveyor.getMessage(ConsoleWebMessages.UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_LINK));
    linkOpen.setResource(new ExternalResource("/console/launch.jnlp"));
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

  }
}
