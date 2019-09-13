package org.openthinclient.web.dashboard;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.ExternalResource;
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
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.service.update.UpdateChecker;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.thinclient.ClientView;
import org.openthinclient.web.ui.PrivacyNoticeInfo;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Supplier;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name= DashboardView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, caption = "Dashboard", order=10)
@ThemeIcon("icon/dashboard.svg")
public class DashboardView extends Panel implements View {

  public final static String NAME = "";
  private static final String NEWS_URL = "https://openthinclient.com/manager_news/?";

  private static final Logger LOGGER = LoggerFactory.getLogger(DashboardView.class);

  @Autowired
  private ClientService clientService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private DeviceService deviceService;
  @Autowired
  private UnrecognizedClientService unrecognizedClientService;
  @Autowired
  private UpdateChecker updateChecker;
  @Autowired
  private PackageManager packageManager;

  @Value("${application.version}")
  private String applicationVersion;

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
                                                           new ThemeResource("icon/thinclient.svg"),
                                                           getInfoContent(() -> clientService.count()));

    InfoContentPanel applicationInfo = new InfoContentPanel(mc.getMessage(UI_APPLICATION_HEADER),
                                                            new ThemeResource("icon/application.svg"),
                                                            getInfoContent(() -> applicationService.count()));
    InfoContentPanel devicesInfo = new InfoContentPanel(mc.getMessage(UI_DEVICE_HEADER),
                                                        new ThemeResource("icon/device.svg"),
                                                        getInfoContent(() -> deviceService.count()));

    dashboardPanels.addComponents(thinclientInfo, applicationInfo, devicesInfo);

    UnregisteredClientsPanel ucp = new UnregisteredClientsPanel("Unregistered " + mc.getMessage(UI_CLIENT_HEADER),
                                                    new ThemeResource("icon/thinclient.svg"));
    dashboardPanels.addComponent(ucp);

    dashboardPanels.addComponent(new UpdatePanel());

    ContentPanel helpPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_TITLE), new ThemeResource("icon/help.svg"));
	  helpPanel.addStyleName("size-1x2");
    helpPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_CONTENT), ContentMode.HTML));

    ContentPanel toolsPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_TITLE), new ThemeResource("icon/meter.svg"));
	  toolsPanel.addStyleName("size-1x2");
    toolsPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_CONTENT), ContentMode.HTML));

    dashboardPanels.addComponents(helpPanel, toolsPanel);

    BrowserFrame newsBrowser = new BrowserFrame(null, new ExternalResource(NEWS_URL + applicationVersion));
    newsBrowser.addStyleNames("size-2x3", "dashboard-panel");
    dashboardPanels.addComponent(newsBrowser);

    return dashboardPanels;
  }

  private String getInfoContent(Supplier<Integer> contentSupplier) {
    String info = "";
    try {
      info = String.valueOf(contentSupplier.get());
    } catch (Exception e) {
      LOGGER.warn("Cannot load content: " + e.getMessage());
    }
    return info;
  }

  class InfoContentPanel extends ContentPanel {

    public InfoContentPanel(String message, ThemeResource themeResource, String caption) {
      super(message, themeResource);
      // setHeight(70, Unit.PIXELS);
      addImageStyleName("dashboard-panel-image-circle");

      if (caption != null) {
        Label label = new Label(caption);
        label.addStyleName("content-panel-number-large");
        addComponent(label);
      }
    }
  }

  class UpdatePanel extends ContentPanel {
    private static final String managerUpdateURL = "/ui/settings#!support";
    private static final String packagesUpdateURL = "/ui/settings#!package-management";

    private Label newVersionLabel = new Label();
    private Label newPackagesLabel = new Label();

    public UpdatePanel() {
      super(mc.getMessage(UI_DASHBOARDVIEW_UPDATE_NOTICE_CAPTION));
      addStyleNames("update-notification", "size-1x2");

      if(updateChecker.hasNetworkError()) {
        Label errorNotification = new Label(mc.getMessage(UI_DASHBOARDVIEW_UPDATE_NOTICE_NETWORK_ERROR));
        errorNotification.setCaption(mc.getMessage(UI_DASHBOARDVIEW_UPDATE_NOTICE_NETWORK_ERROR_CAPTION));
        errorNotification.setIcon(VaadinIcons.EXCLAMATION_CIRCLE_O);
        errorNotification.setStyleName("update-error");
        addComponents(errorNotification);
      } else {
        CssLayout managerNotification = new CssLayout();
        managerNotification.setStyleName("manager-updates");
        CssLayout packagesNotification = new CssLayout();
        packagesNotification.setStyleName("package-updates");
        addComponents(managerNotification, packagesNotification);

        managerNotification.addComponents(
          new Label("openthinclient-Manager " + applicationVersion),
          new Link("Manager Updates", new ExternalResource(managerUpdateURL)),
          newVersionLabel
        );
        updateManagerStatus(updateChecker.getNewVersion());

        packagesNotification.addComponents(
          new Label("openthinclient-OS"),
          new Link(mc.getMessage(UI_PACKAGEMANAGERMAINNAVIGATORVIEW_CAPTION), new ExternalResource(packagesUpdateURL)),
          newPackagesLabel
        );
        updatePackageStatus(packageManager.getUpdateablePackages());
      }
    }

    void updateManagerStatus(Optional<String> newVersion) {
      if(newVersion.isPresent()) {
        newVersionLabel.setCaption(mc.getMessage(UI_DASHBOARDVIEW_UPDATE_NOTICE_MANAGER_UPDATABLE, newVersion.get()));
        newVersionLabel.setIcon(VaadinIcons.EXCLAMATION_CIRCLE_O);
      } else {
        newVersionLabel.setCaption(mc.getMessage(UI_DASHBOARDVIEW_UPDATE_NOTICE_MANAGER_CURRENT));
        newVersionLabel.setIcon(VaadinIcons.CHECK);
      }
    }

    void updatePackageStatus(Collection updatablePackages) {
      if(updatablePackages.size() > 0) {
        newPackagesLabel.setCaption(mc.getMessage(UI_DASHBOARDVIEW_UPDATE_NOTICE_PACKAGES_UPDATABLE));
        newPackagesLabel.setIcon(VaadinIcons.EXCLAMATION_CIRCLE_O);
      } else {
        newPackagesLabel.setCaption(mc.getMessage(UI_DASHBOARDVIEW_UPDATE_NOTICE_PACKAGES_CURRENT));
        newPackagesLabel.setIcon(VaadinIcons.CHECK);
      }
    }
  }

  class UnregisteredClientsPanel extends ContentPanel {

    public UnregisteredClientsPanel(String title, Resource resource) {
      super(title, resource);
      addStyleName("unregistered-clients");
      setSpacing(false);
      // setHeight(120, Unit.PIXELS);
      addImageStyleName("dashboard-panel-image-circle");

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
