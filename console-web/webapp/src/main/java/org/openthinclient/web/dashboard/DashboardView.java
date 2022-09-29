package org.openthinclient.web.dashboard;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.service.update.UpdateChecker;
import org.openthinclient.service.common.license.License;
import org.openthinclient.service.common.license.LicenseManager;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
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

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name= DashboardView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, caption = "Dashboard", order=10)
@ThemeIcon("icon/dashboard.svg")
public class DashboardView extends Panel implements View {

  public final static String NAME = "";
  private static final String NEWS_URL = "https://openthinclient.com/manager_news/";

  private static final Logger LOGGER = LoggerFactory.getLogger(DashboardView.class);

  @Autowired
  private ClientService clientService;
  @Autowired
  private LicenseManager licenseManager;
  @Autowired
  private UnrecognizedClientService unrecognizedClientService;
  @Autowired
  private UpdateChecker updateChecker;
  @Autowired
  private PackageManager packageManager;

  @Value("${application.version}")
  private String applicationVersion;
  @Value("${application.is-preview}")
  private boolean applicationIsPreview;

  private EventBus.SessionEventBus eventBus;
  private final IMessageConveyor mc;
  private CssLayout dashboardPanels;

  @Autowired
  public DashboardView(EventBus.SessionEventBus eventBus) {
    setSizeFull();
    mc = new MessageConveyor(UI.getCurrent().getLocale());
    this.eventBus = eventBus;
  }

  @PostConstruct
  public void init() {
    Layout root = new CssLayout();
    root.addComponents(buildContent(), new PrivacyNoticeInfo());
    setContent(root);
  }

  private Component buildContent() {
    dashboardPanels = new CssLayout();
    dashboardPanels.addStyleName("dashboard-panels");

    ContentPanel helpPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_TITLE), new ThemeResource("icon/help.svg"));
	  helpPanel.addStyleName("size-1x2");
    helpPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_CONTENT), ContentMode.HTML));

    ContentPanel toolsPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_TITLE), new ThemeResource("icon/meter.svg"));
	  toolsPanel.addStyleName("size-1x2");
    toolsPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_CONTENT), ContentMode.HTML));

    dashboardPanels.addComponents(
      new UpdatePanel(),
      new LicensePanel(),
      new UnregisteredClientsPanel(),
      helpPanel,
      toolsPanel,
      new NewsBrowser()
    );

    return dashboardPanels;
  }

  class LicensePanel extends ContentPanel {
    private static final String licenseManagerURL = "/ui/settings#!license";

    public LicensePanel() {
      super(mc.getMessage(UI_DASHBOARDVIEW_LICENSE_INFO_CAPTION));
      addStyleName("license-info");

      License license = licenseManager.getLicense();

      Integer maxCount = 24;
      if(license != null) {
        maxCount = license.getCount();
      }
      Integer clientCount = clientService.count();
      String countText = mc.getMessage(UI_DASHBOARDVIEW_LICENSE_INFO_CLIENT_COUNT, clientCount, maxCount);
      Label countLabel = new Label(countText, ContentMode.HTML);
      countLabel.addStyleName("client-count");
      if (maxCount < clientCount) {
        countLabel.addStyleName("too-many");
      } else if (maxCount - clientCount < 11) {
        countLabel.addStyleName("warn");
      }

      License.State licenseState = licenseManager.getLicenseState(clientCount);
      ConsoleWebMessages licenseStatusKey;
      String licenseStatusClass;
      switch (licenseState) {
        case COMMUNITY:
          licenseStatusKey = UI_DASHBOARDVIEW_LICENSE_INFO_COMMUNITY_LICENSE;
          licenseStatusClass = "ok";
          break;
        case OK:
          licenseStatusKey = UI_DASHBOARDVIEW_LICENSE_INFO_LICENSE_OK;
          licenseStatusClass = "ok";
          break;
        default:
          licenseStatusKey = UI_DASHBOARDVIEW_LICENSE_INFO_LICENSE_PROBLEM;
          licenseStatusClass = "problem";
      }
      Label licenseLabel = new Label(mc.getMessage(licenseStatusKey));
      licenseLabel.addStyleNames("license-status", licenseStatusClass);

      Link licenseManagerLink = new Link(
        mc.getMessage(UI_DASHBOARDVIEW_LICENSE_INFO_LINK),
        new ExternalResource(licenseManagerURL));

      addComponents(countLabel, licenseLabel, licenseManagerLink);
    }
  }

  class UpdatePanel extends ContentPanel {
    private static final String managerUpdateURL = "/ui/settings#!support";
    private static final String packagesUpdateURL = "/ui/settings#!package-management";

    private Label newVersionLabel = new Label();
    private Label newPackagesLabel = new Label();

    public UpdatePanel() {
      super(mc.getMessage(UI_DASHBOARDVIEW_UPDATE_NOTICE_CAPTION), new ThemeResource("icon/bell.svg"));
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
        updatePackageStatus(packageManager.getUpdateablePackages(applicationIsPreview));
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

    void updatePackageStatus(Collection<Package> updatablePackages) {
      if(updatablePackages.size() > 0) {
        newPackagesLabel.setCaption(mc.getMessage(UI_DASHBOARDVIEW_UPDATE_NOTICE_PACKAGES_UPDATABLE));
        newPackagesLabel.setIcon(VaadinIcons.EXCLAMATION_CIRCLE_O);
      } else {
        newPackagesLabel.setCaption(mc.getMessage(UI_DASHBOARDVIEW_UPDATE_NOTICE_PACKAGES_CURRENT));
        newPackagesLabel.setIcon(VaadinIcons.CHECK);
      }
    }
  }

  class NewsBrowser extends CssLayout {
    public NewsBrowser() {
      super();
      addStyleNames("news-browser", "dashboard-panel", "size-2x3");

      BrowserFrame frame = new BrowserFrame(null, new ExternalResource("about:blank"));

      CssLayout fallback = new CssLayout();
      fallback.addComponents(
        new Image(null, new ThemeResource("open_news.png")),
        new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_NEW_BROWSER_FALLBACK), ContentMode.HTML)
        );
      fallback.addStyleName("fallback");

      addComponents(frame, fallback);

      JavaScript.getCurrent().execute(
          String.format("loadBrowserFrame('.news-browser .v-browserframe', '%s?%s/%s')",
                        NEWS_URL, applicationVersion, UI.getCurrent().getLocale().getLanguage()));
    }
  }

  class UnregisteredClientsPanel extends ContentPanel {
    private ComboBox<String> macCombo;

    public UnregisteredClientsPanel() {
      super(mc.getMessage(UI_DASHBOARDVIEW_UNREGISTERED_CLIENTS));
      addStyleName("unregistered-clients");

      macCombo = new ComboBox<>();
      macCombo.setPlaceholder(mc.getMessage(UI_THINCLIENT_MAC));
      macCombo.setEmptySelectionAllowed(false);
      macCombo.setNewItemProvider(input -> {
        openClientView(input);
        return Optional.empty();
      });
      macCombo.addValueChangeListener(event -> openClientView(event.getValue()));
      this.updatePXEClientList();

      Button updateButton = new Button(mc.getMessage(UI_DASHBOARDVIEW_UNREGISTERED_CLIENTS_UPDATE_BUTTON));
      updateButton.addStyleName("dashboard-panel-unregistered-clients-button");
      updateButton.setIcon(VaadinIcons.REFRESH);
      updateButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
      updateButton.addClickListener( event -> this.updatePXEClientList() );

      Button forgetButton = new Button(mc.getMessage(UI_DASHBOARDVIEW_UNREGISTERED_CLIENTS_FORGET_BUTTON));
      forgetButton.addStyleName("dashboard-panel-unregistered-clients-clean-button");
      forgetButton.setIcon(VaadinIcons.TRASH);
      forgetButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
      forgetButton.addClickListener(event -> {
        unrecognizedClientService.findAll().forEach(directoryObject -> {
          Realm realm = directoryObject.getRealm();
          try {
            realm.getDirectory().delete(directoryObject);
          } catch (DirectoryException e) {
            LOGGER.error("Cannot delete unrecognizedClient: " + directoryObject, e);
          }
        });
        this.updatePXEClientList();
      });

      addComponents(macCombo, updateButton, forgetButton);

      eventBus.subscribeWithWeakReference(this);
    }

    private void openClientView(String macaddress) {
      macaddress = macaddress.trim();
      if (macaddress.matches("^[0-9a-fA-F]{12}$")) {
        macaddress = String.join(":", macaddress.split("(?<=\\G..)"));
      } else if (!macaddress.matches("^([0-9A-Fa-f]{2}[:]){5}[0-9A-Fa-f]{2}$")) {
        return;
      }
      if(clientService.findByHwAddress(macaddress).size() == 0) {
        UI.getCurrent().getNavigator().navigateTo(ClientView.NAME + "/register/" + macaddress);
      }
    }

    private void updatePXEClientList() {
      macCombo.setItems(unrecognizedClientService.getLastSeenMACs(50));
    }

    @EventBusListenerMethod
    public void updatePXEClientList(final DashboardEvent.PXEClientListRefreshEvent event) {
      this.updatePXEClientList();
    }
  }
}
