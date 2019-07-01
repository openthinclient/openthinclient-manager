package org.openthinclient.web.dashboard;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.Resource;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.thinclient.ClientView;
import org.openthinclient.web.ui.PrivacyNoticeInfo;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.function.Supplier;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name= DashboardView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, caption = "Dashboard", order=10)
@ThemeIcon("icon/dashboard.svg")
public class DashboardView extends Panel implements View {

  public final static String NAME = "";

  private static final Logger LOGGER = LoggerFactory.getLogger(DashboardView.class);

  @Autowired
  private ClientService clientService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private DeviceService deviceService;
  @Autowired
  private UnrecognizedClientService unrecognizedClientService;

  private EventBus.SessionEventBus eventBus;
  private final IMessageConveyor mc;
  private CssLayout dashboardPanels;
  private ComboBox<UnrecognizedClient> macCombo;

  @Autowired
  public DashboardView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
    setSizeFull();
    mc = new MessageConveyor(UI.getCurrent().getLocale());
    this.eventBus = eventBus;
    eventBus.publish(this, new DashboardEvent.UpdateHeaderLabelEvent(mc.getMessage(UI_DASHBOARDVIEW_HEADER)));
  }

  @PostConstruct
  public void init() {
    VerticalLayout root = new VerticalLayout();
    root.addComponents(buildContent(), new PrivacyNoticeInfo());
    setContent(root);
  }

  private Component buildContent() {
    dashboardPanels = new CssLayout();
    dashboardPanels.addStyleName("dashboard-panels");
    Responsive.makeResponsive(dashboardPanels);

    InfoContentPanel thinclientInfo = new InfoContentPanel(mc.getMessage(UI_CLIENT_HEADER),
                                                           new ThemeResource("icon/logo-white.svg"),
                                                           getInfoContent(() -> clientService.findAll()));

    InfoContentPanel applicationInfo = new InfoContentPanel(mc.getMessage(UI_APPLICATION_HEADER),
                                                            new ThemeResource("icon/packages-white.svg"),
                                                            getInfoContent(() -> applicationService.findAll()));
    InfoContentPanel devicesInfo = new InfoContentPanel(mc.getMessage(UI_DEVICE_HEADER),
                                                        new ThemeResource("icon/display-white.svg"),
                                                        getInfoContent(() -> deviceService.findAll()));

    dashboardPanels.addComponents(thinclientInfo, applicationInfo, devicesInfo);

    UnregisteredClientsPanel ucp = new UnregisteredClientsPanel("Unregistered " + mc.getMessage(UI_CLIENT_HEADER),
                                                    new ThemeResource("icon/logo-white.svg"));
    dashboardPanels.addComponent(ucp);

    ContentPanel helpPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_TITLE), new ThemeResource("icon/help.svg"));
    helpPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_CONTENT), ContentMode.HTML));

    ContentPanel otcPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_OTC_TITLE), new ThemeResource("icon/logo.svg"));
    otcPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_OTC_CONTENT), ContentMode.HTML));

    ContentPanel toolsPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_TITLE), new ThemeResource("icon/meter.svg"));
    toolsPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_CONTENT), ContentMode.HTML));

    dashboardPanels.addComponents(helpPanel, otcPanel, toolsPanel);

    return dashboardPanels;
  }

  private String getInfoContent(Supplier<Set> contentSupplier) {
    String info = "";
    try {
      info = String.valueOf(contentSupplier.get().size());
    } catch (Exception e) {
      LOGGER.warn("Cannot load content: " + e.getMessage());
    }
    return info;
  }

  class InfoContentPanel extends ContentPanel {

    public InfoContentPanel(String message, ThemeResource themeResource, String caption) {
      super(message, themeResource);
      setHeight(70, Unit.PIXELS);
      addImageStyleName("dashboard-panel-image-circle");

      if (caption != null) {
        Label label = new Label(caption);
        label.addStyleName("content-panel-number-large");
        addComponent(label);
      }
    }
  }

  class UnregisteredClientsPanel extends ContentPanel {

    public UnregisteredClientsPanel(String title, Resource resource) {
      super(title, resource);
      setSpacing(false);
      setHeight(120, Unit.PIXELS);
      addImageStyleName("dashboard-panel-unregistered-clients-image-circle");

      // Selection ComboBox
      macCombo = new ComboBox<>();
      macCombo.setPlaceholder(mc.getMessage(UI_THINCLIENT_MAC));
      macCombo.setEmptySelectionAllowed(false);
      try {
        macCombo.setDataProvider(new ListDataProvider<>(unrecognizedClientService.findAll()));
      } catch (Exception e) {
        LOGGER.warn("Cannot load content: " + e.getMessage());
      }
      macCombo.setItemCaptionGenerator(UnrecognizedClient::getMacAddress);
      macCombo.addValueChangeListener(event ->
          UI.getCurrent().getNavigator().navigateTo(ClientView.NAME + "/register/" + event.getValue().getMacAddress())
      );

      HorizontalLayout hl = new HorizontalLayout();
      Button btn = new Button(mc.getMessage(UI_PACKAGESOURCES_BUTTON_UPDATE_CAPTION));
      btn.addStyleName("dashboard-panel-unregistered-clients-button");
      btn.setIcon(VaadinIcons.REFRESH);
      btn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
      btn.addClickListener(event -> {
        try {
          macCombo.setDataProvider(new ListDataProvider<>(unrecognizedClientService.findAll()));
        } catch (Exception e) {
          LOGGER.warn("Cannot load content: " + e.getMessage());
        }
      });
      Button btnCleanClients = new Button();
      btnCleanClients.addStyleName("dashboard-panel-unregistered-clients-clean-button");
      btnCleanClients.setIcon(VaadinIcons.TRASH);
      btnCleanClients.addStyleName(ValoTheme.BUTTON_BORDERLESS);
      btnCleanClients.addClickListener(event -> {
        // TODO: ist doch voll arm, das wollen wir nicht wirklich hier (eigener Service?)
        try {
          unrecognizedClientService.findAll().forEach(directoryObject -> {
            Realm realm = directoryObject.getRealm();
            try {
              realm.getDirectory().delete(directoryObject);
            } catch (DirectoryException e) {
              LOGGER.info("Cannot delete unrecognizedClient: " + directoryObject + ": " + e.getMessage());
            }
          });
          macCombo.setDataProvider(new ListDataProvider<>(unrecognizedClientService.findAll()));
        } catch (Exception e) {
          LOGGER.warn("Cannot load content: " + e.getMessage());
        }
      });

      hl.addComponents(btn, btnCleanClients);
      addComponent(hl);
      addComponent(macCombo);

    }
  }

  @EventBusListenerMethod
  public void updatePXEClientList(final DashboardEvent.PXEClientListRefreshEvent event) {
    try {
      Set<UnrecognizedClient> clients = unrecognizedClientService.findAll();
      LOGGER.debug("Update PXE-client list, size {}", clients.size());
      macCombo.setDataProvider(new ListDataProvider<>(clients));
    } catch (Exception e) {
      LOGGER.warn("Cannot load content: " + e.getMessage());
    }
  }

  @Override
  public void attach() {
    super.attach();
    eventBus.subscribe(this);
  }

  @Override
  public void detach() {
    eventBus.unsubscribe(this);
    super.detach();
  }

}
