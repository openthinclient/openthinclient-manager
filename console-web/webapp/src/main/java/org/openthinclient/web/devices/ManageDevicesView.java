package org.openthinclient.web.devices;

import com.vaadin.navigator.View;
import com.vaadin.server.*;
import com.vaadin.shared.ui.BorderStyle;
import com.vaadin.shared.ui.ContentMode;
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

import org.openthinclient.api.rest.appliance.TokenManager;
import org.openthinclient.service.common.home.impl.ApplianceConfiguration;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.novnc.NoVNCComponent;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;

import ch.qos.cal10n.MessageConveyor;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_HEADER;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_DEVICEMANAGEMENT_HEADER;

@SpringView(name = "devices")
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode = "UI_DEVICEMANAGEMENT_HEADER", order = -100)
public class ManageDevicesView extends Panel implements View {

  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = -8836200902351197949L;

  final MessageConveyor mc;
  final VerticalLayout root;

  @Autowired
  ApplianceConfiguration applianceConfiguration;

  @Autowired
  TokenManager tokenManager;


  public ManageDevicesView() {

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    setSizeFull();

    root = new VerticalLayout();
    root.setSizeFull();
    root.setMargin(true);
    root.addStyleName("mainview");
    setContent(root);
    Responsive.makeResponsive(root);

    root.addComponent(new ViewHeader(mc.getMessage(UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_HEADER)));
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
    if (applianceConfiguration.isEnabled())
      return buildApplianceContent();
    else
      return buildPlaceholderContent();
  }

  private Component buildApplianceContent() {
    String host = applianceConfiguration.getNoVNCConsoleHostname();
    if (host == null || host.trim().isEmpty())
      host = UI.getCurrent().getPage().getLocation().getHost();

    Link linkOpen = new Link();
    linkOpen.setCaption(mc.getMessage(ConsoleWebMessages.UI_DEVICEMANAGEMENT_CONSOLE_ABOUT_JNLP_LINK));
    linkOpen.setResource(new ExternalResource("/console/launch.jnlp"));

    // javascript components seem to be unable to resolve theme resources.
    // due to this (and as a temporary workaround), we're specifying the full path here
    // FIXME eiter remove novnc as a theme resource or make NoVNCComponent able to resolve theme resources
    ExternalResource tr = new ExternalResource("/VAADIN/themes/dashboard/novnc/vnc.html?host=" + host +
        "&port=" + applianceConfiguration.getNoVNCConsolePort() +
        "&encrypt=" + (applianceConfiguration.isNoVNCConsoleEncrypted() ? "1" : "0") +
        "&allowfullscreen=" + applianceConfiguration.isNoVNCConsoleAllowfullscreen() +
        "&resize=" + applianceConfiguration.getNoVNCResizeMode() +
        "&autoconnect=" + applianceConfiguration.isNoVNCConsoleAutoconnect()+
        "&path=?token=" + tokenManager.createToken(VaadinRequest.getCurrent().getRemoteAddr())

    );
    Link vncInNewWindow = new Link();
    vncInNewWindow.setCaption(mc.getMessage(ConsoleWebMessages.UI_DEVICEMANAGEMENT_CONSOLE_VNC_LINK));
    vncInNewWindow.setResource(tr);
    vncInNewWindow.setTargetName("_blank");
    vncInNewWindow.setTargetHeight(600);
    vncInNewWindow.setTargetWidth(800);
    vncInNewWindow.setTargetBorder(BorderStyle.DEFAULT);

    VerticalLayout verticalLayout = new VerticalLayout();
    verticalLayout.setMargin(false);
    verticalLayout.setSpacing(true);
    verticalLayout.addComponents(linkOpen, vncInNewWindow);

    return verticalLayout;

  }

  private Component buildPlaceholderContent() {
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

}
