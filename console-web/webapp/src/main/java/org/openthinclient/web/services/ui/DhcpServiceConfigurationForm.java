package org.openthinclient.web.services.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.Binder;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.util.Config.BootOptions.PXEServicePolicyType;
import org.openthinclient.service.common.ManagedService;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.ServiceManager;
import org.openthinclient.service.dhcp.DHCPService;
import org.openthinclient.service.dhcp.DhcpServiceConfiguration;
import org.openthinclient.web.Audit;
import org.openthinclient.web.SchemaService;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.PostConstruct;

import org.openthinclient.service.dhcp.DhcpServiceConfiguration.PXEPolicy;
import org.openthinclient.service.dhcp.DhcpServiceConfiguration.PXEType;

@SpringComponent
@UIScope
class DhcpServiceConfigurationForm extends FormLayout {
  @Autowired
  private SchemaService schemaService;
  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private ServiceManager serviceManager;
  private ManagedService<DHCPService, DhcpServiceConfiguration> managedDhcpService;
  private final Binder<DhcpServiceConfiguration> binder = new Binder<>();
  private final IMessageConveyor mc;
  private final EnumMessageConveyorCaptionGenerator<PXEType, ConsoleWebMessages> typeCaptionGenerator;
  private final EnumMessageConveyorCaptionGenerator<PXEPolicy, ConsoleWebMessages> policyCaptionGenerator;

  public DhcpServiceConfigurationForm() {
    mc = new MessageConveyor(UI.getCurrent().getLocale());

    typeCaptionGenerator = (new EnumMessageConveyorCaptionGenerator<PXEType, ConsoleWebMessages>(mc))
      .addMapping(PXEType.AUTO, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE_AUTO)
      .addMapping(PXEType.BIND_TO_ADDRESS, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE_BIND_TO_ADDRESS)
      .addMapping(PXEType.EAVESDROPPING, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE_EAVESDROPPING)
      .addMapping(PXEType.SINGLE_HOMED, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE_SINGLE_HOMED)
      .addMapping(PXEType.SINGLE_HOMED_BROADCAST, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE_SINGLE_HOMED_BROADCAST);

    policyCaptionGenerator = (new EnumMessageConveyorCaptionGenerator<PXEPolicy, ConsoleWebMessages>(mc))
      .addMapping(PXEPolicy.ANY_CLIENT, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXEPOLICY_ANY_CLIENT)
      .addMapping(PXEPolicy.ONLY_CONFIGURED, ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXEPOLICY_ONLY_CONFIGURED);
  }

  @PostConstruct
  void init() {
    managedDhcpService = serviceManager.getManagedService(DHCPService.class);
    buildContent();
  }

  public void refresh() {
    binder.setBean((DhcpServiceConfiguration) managedDhcpService.getService().getConfiguration());
  }

  void buildContent() {
    final Label label = new Label(mc.getMessage(ConsoleWebMessages.UI_SERVICE_DHCP_CONF_CAPTION));
    label.addStyleName(ValoTheme.LABEL_H2);

    final NativeSelect<PXEType> typeSelect = new NativeSelect<PXEType>(mc.getMessage(ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXETYPE));
    typeSelect.setItems(PXEType.values());
    typeSelect.setItemCaptionGenerator(typeCaptionGenerator);
    typeSelect.setEmptySelectionAllowed(false);
    binder.forField(typeSelect).bind( conf -> conf.getPxe().getType(),
                                      (conf, type) -> conf.getPxe().setType(type));

    final CheckBox trackClientsCheckbox = new CheckBox(mc.getMessage(ConsoleWebMessages.UI_SERVICE_DHCP_CONF_TRACK_CLIENTS));
    binder.forField(trackClientsCheckbox).bind( DhcpServiceConfiguration::isTrackUnrecognizedPXEClients,
                                                DhcpServiceConfiguration::setTrackUnrecognizedPXEClients);

    final NativeSelect<PXEPolicy> policySelect = new NativeSelect<PXEPolicy>(mc.getMessage(ConsoleWebMessages.UI_SERVICE_DHCP_CONF_PXEPOLICY));
    policySelect.setItems(PXEPolicy.values());
    policySelect.setItemCaptionGenerator(policyCaptionGenerator);
    policySelect.setEmptySelectionAllowed(false);
    binder.forField(policySelect).bind( conf -> conf.getPxe().getPolicy(),
                                        (conf, policy) -> conf.getPxe().setPolicy(policy));

    final Button saveButton = new Button(mc.getMessage(ConsoleWebMessages.UI_BUTTON_SAVE), e -> {
      Audit.logSave("Service settings");

      binder.writeBeanIfValid((DhcpServiceConfiguration)managedDhcpService.getService().getConfiguration());

      // save the appropriate service configuration.
      managerHome.save(managedDhcpService.getService().getConfigurationClass());

      PXEPolicy pxePolicy = policySelect.getValue();
      schemaService.saveTftpPolicy(
         pxePolicy == PXEPolicy.ANY_CLIENT? PXEServicePolicyType.AnyClient
                                          : PXEServicePolicyType.RegisteredOnly
      );

      if(managedDhcpService.isRunning()) {
        managedDhcpService.restart();
      }
    });

    addComponent(label);
    addComponent(typeSelect);
    addComponent(trackClientsCheckbox);
    addComponent(policySelect);
    addComponent(saveButton);
  }
}
