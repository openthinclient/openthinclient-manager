package org.openthinclient.web.services.ui;

import com.vaadin.data.Binder;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

import org.openthinclient.service.common.ManagedService;
import org.openthinclient.service.common.ServiceManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.dhcp.DhcpService;
import org.openthinclient.service.dhcp.DhcpServiceConfiguration;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.Sparklines;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import ch.qos.cal10n.MessageConveyor;

@SpringView(name = "service-dhcp")
@SideBarItem(sectionId = DashboardSections.SERVICE_MANAGEMENT, captionCode = "UI_SERVICE_DHCP_CAPTION")
public class DhcpServiceConfigurationView extends Panel implements View {
   
  private static final Logger LOGGER = LoggerFactory.getLogger(DhcpServiceConfigurationView.class);

  private final ManagedService<DhcpService, DhcpServiceConfiguration> service;
  private final MessageConveyor conveyor;
  private final Button startButton;
  private final Button stopButton;
  private final NativeSelect<DhcpServiceConfiguration.PXEType> typeSelect;
  private final CheckBox trackClientsCheckbox;
  private final NativeSelect<DhcpServiceConfiguration.PXEPolicy> policySelect;
  private final Button saveButton;

  private final Binder<DhcpServiceConfiguration> binder;

  @Autowired
  public DhcpServiceConfigurationView(ServiceManager serviceManager, ManagerHome managerHome) {
     
    conveyor = new MessageConveyor(UI.getCurrent().getLocale());
    service = serviceManager.getManagedService(DhcpService.class);

    addStyleName(ValoTheme.PANEL_BORDERLESS);
    setSizeFull();
    DashboardEventBus.register(this);

    VerticalLayout root = new VerticalLayout();
    root.setSizeFull();
    root.setMargin(true);
    root.addStyleName("dashboard-view");
    setContent(root);
    Responsive.makeResponsive(root);

    root.addComponent(new ViewHeader(conveyor.getMessage(UI_SERVICE_DHCP_CAPTION)));

    final ViewHeader header = new ViewHeader("", false);
    header.addTool(this.startButton = new Button(conveyor.getMessage(ConsoleWebMessages.UI_SERVICE_START)));
    header.addTool(this.stopButton = new Button(conveyor.getMessage(ConsoleWebMessages.UI_SERVICE_STOP)));
    startButton.setIcon(FontAwesome.PLAY);
    startButton.setStyleName(ValoTheme.BUTTON_FRIENDLY);
    stopButton.setIcon(FontAwesome.STOP);
    stopButton.setStyleName(ValoTheme.BUTTON_DANGER);

    root.addComponent(header);

    // Content
    final FormLayout formLayout = new FormLayout();
//    fieldGroup = new BeanFieldGroup<>(DhcpServiceConfiguration.class);
    binder = new Binder<>();

//    typeSelect = new EnumSelect<>(conveyor.getMessage(ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE));
    typeSelect = new NativeSelect<DhcpServiceConfiguration.PXEType>(conveyor.getMessage(ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE));
    typeSelect.setItems(DhcpServiceConfiguration.PXEType.values());
    final EnumMessageConveyorCaptionGenerator<DhcpServiceConfiguration.PXEType, ConsoleWebMessages> captionGenerator = new EnumMessageConveyorCaptionGenerator<>(conveyor);
    captionGenerator.addMapping(DhcpServiceConfiguration.PXEType.AUTO, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE_AUTO);
    captionGenerator.addMapping(DhcpServiceConfiguration.PXEType.BIND_TO_ADDRESS, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE_BIND_TO_ADDRESS);
    captionGenerator.addMapping(DhcpServiceConfiguration.PXEType.EAVESDROPPING, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE_EAVESDROPPING);
    captionGenerator.addMapping(DhcpServiceConfiguration.PXEType.SINGLE_HOMED, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE_SINGLE_HOMED);
    captionGenerator.addMapping(DhcpServiceConfiguration.PXEType.SINGLE_HOMED_BROADCAST, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE_SINGLE_HOMED_BROADCAST);
    typeSelect.setItemCaptionGenerator(captionGenerator);
//    typeSelect.setRequired(true);
    typeSelect.setEmptySelectionAllowed(false);
    binder.bind(typeSelect, "pxe.type");

    trackClientsCheckbox = new CheckBox(conveyor.getMessage(ConsoleWebMessages.UI_SERVICE_DHCP_CONF_TRACK_CLIENTS));
    binder.bind(trackClientsCheckbox, "trackUnrecognizedPXEClients");

    policySelect = new NativeSelect<DhcpServiceConfiguration.PXEPolicy>(conveyor.getMessage(ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXEPOLICY));
    policySelect.setItems(DhcpServiceConfiguration.PXEPolicy.values());
    final EnumMessageConveyorCaptionGenerator<DhcpServiceConfiguration.PXEPolicy, ConsoleWebMessages> policyCaptionGenerator = new EnumMessageConveyorCaptionGenerator<>(conveyor);
    policyCaptionGenerator.addMapping(DhcpServiceConfiguration.PXEPolicy.ANY_CLIENT, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXEPOLICY_ANY_CLIENT);
    policyCaptionGenerator.addMapping(DhcpServiceConfiguration.PXEPolicy.ONLY_CONFIGURED, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXEPOLICY_ONLY_CONFIGURED);
    policySelect.setItemCaptionGenerator(policyCaptionGenerator);
//    policySelect.setRequired(true);
    policySelect.setEmptySelectionAllowed(false);
    binder.bind(policySelect, "pxe.policy");

    final Label configCaptionLabel = new Label(conveyor.getMessage(ConsoleWebMessages.UI_SERVICE_DHCP_CONF_CAPTION));
    configCaptionLabel.addStyleName(ValoTheme.LABEL_H2);
    configCaptionLabel.addStyleName(ValoTheme.LABEL_COLORED);
    formLayout.addComponent(configCaptionLabel);

    formLayout.addComponent(typeSelect);
    formLayout.addComponent(trackClientsCheckbox);
    formLayout.addComponent(policySelect);

    saveButton = new Button(conveyor.getMessage(ConsoleWebMessages.UI_BUTTON_SAVE));
    formLayout.addComponent(new HorizontalLayout(saveButton));

    saveButton.addClickListener(e -> {
//      try {
//        fieldGroup.commit();
//      } catch (FieldGroup.CommitException e1) {
//        LOGGER.error("Failed to commit dhcp configuration", e1);
//        // FIXME some kind of user feedback required.
//        return;
//      }
      binder.writeBeanIfValid((DhcpServiceConfiguration) service.getService().getConfiguration());

      // save the appropriate service configuration.
      managerHome.save(service.getService().getConfigurationClass());

      service.restart();

    });

    root.addComponent(formLayout);
    root.setExpandRatio(formLayout, 1);

  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    startButton.setEnabled(!service.isRunning());
    stopButton.setEnabled(service.isRunning());

//    fieldGroup.setItemDataSource((DhcpServiceConfiguration) service.getService().getConfiguration());
    binder.setBean((DhcpServiceConfiguration) service.getService().getConfiguration());
  }
}
