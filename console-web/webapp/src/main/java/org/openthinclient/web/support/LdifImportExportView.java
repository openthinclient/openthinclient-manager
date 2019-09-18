package org.openthinclient.web.support;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Responsive;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import org.openthinclient.api.ldif.export.LdifExporterService;
import org.openthinclient.api.ldif.export.LdifImporterService;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.config.NetworkConfiguration.ProxyConfiguration;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.filebrowser.FileUploadSubWindow;
import org.openthinclient.web.filebrowser.FileUploadView;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.SettingsUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_LDIF_IMPORT_EXPORT_HEADER;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_PROXY_CONFIGURATION_HEADER;

@SpringView(name = "ldif-import-export", ui = SettingsUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SUPPORT_LDIF_IMPORT_EXPORT_HEADER", order = 71)
public class LdifImportExportView extends Panel implements View, FileUploadView {

  private static final Logger LOGGER = LoggerFactory.getLogger(LdifImportExportView.class);

  @Autowired
  protected ManagerHome managerHome;
//  @Autowired
//  private LDAPConnectionDescriptor ldapConnectionDescriptor;
  @Autowired
  private RealmService realmService;
  @Autowired
  private DownloadManager downloadManager;

  final MessageConveyor mc;
  final VerticalLayout root ;
  private Path uploadDir;

  private Label successLabel;
  private Label errorLabel;
  private FileUploadSubWindow fileUploadWindow;

  public LdifImportExportView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());
    setSizeFull();
    eventBus.publish(this, new DashboardEvent.UpdateHeaderLabelEvent(mc.getMessage(UI_SUPPORT_LDIF_IMPORT_EXPORT_HEADER)));

    root = new VerticalLayout();
    root.setSizeFull();
    root.setMargin(true);
    setContent(root);
    Responsive.makeResponsive(root);

  }

  @Override
  public String getCaption() {
     return mc.getMessage(UI_SUPPORT_LDIF_IMPORT_EXPORT_HEADER);
  }

  @PostConstruct
  private void init() {

      buildContent();
  }
  
  private void buildContent() {

    VerticalLayout content = new VerticalLayout();

    successLabel = new Label("Aktion erfolgreich", ContentMode.HTML);
    successLabel.setVisible(false);
    errorLabel = new Label("Aktion fehlgeschlagen", ContentMode.HTML);
    errorLabel.setVisible(false);
    errorLabel.setStyleName("unexpected_error");

    final Label labelImportDescription = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LDIF_IMPORT_DESCRIPTION), ContentMode.HTML);
    content.addComponent(labelImportDescription);
    this.uploadDir = managerHome.getLocation().toPath();
    Button uploadButton = new Button("LDIF Import" /*, new ThemeResource(icon)*/);
    uploadButton.addClickListener(e -> {
          successLabel.setVisible(false);
          errorLabel.setVisible(false);
          UI.getCurrent().addWindow(fileUploadWindow = new FileUploadSubWindow(this, uploadDir, managerHome.getLocation().toPath()));
        }
    );
    uploadButton.addStyleName("thinclient-action-button");
    uploadButton.setIcon(VaadinIcons.UPLOAD);
//    uploadButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    content.addComponent(uploadButton);

    content.addComponent(new Label("<hr/>",ContentMode.HTML));

    final Label labelExportDescription = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LDIF_EXPORT_DESCRIPTION), ContentMode.HTML);
    content.addComponent(labelExportDescription);

    this.uploadDir = managerHome.getLocation().toPath();
    Button exportButton = new Button("LDIF Export" /*, new ThemeResource(icon)*/);
    exportButton.addClickListener(e -> {
          successLabel.setVisible(false);
          errorLabel.setVisible(false);
        }
    );
    exportButton.addStyleName("thinclient-action-button");
    exportButton.setIcon(VaadinIcons.DOWNLOAD);
    // attach file-downloader
    FileDownloader fileDownloader = new FileDownloader(createResource());
    fileDownloader.extend(exportButton);
    content.addComponent(exportButton);

    content.addComponents(successLabel, errorLabel);

    root.addComponent(content);
    root.setExpandRatio(content, 1);
  }


  private StreamResource createResource() {
    return new StreamResource((StreamResource.StreamSource) () -> {
      LdifExporterService ldifExporterService = new LdifExporterService(realmService.getDefaultRealm().getConnectionDescriptor());
      return new ByteArrayInputStream(ldifExporterService.performAction(Collections.singleton("")));
    }, "openthinclient-export.ldif");
  }

  public void uploadSucceed(Path file) {
    UI.getCurrent().removeWindow(fileUploadWindow);
    LdifImporterService lis = new LdifImporterService(realmService.getDefaultRealm().getConnectionDescriptor());
    lis.importAction(file.toFile());

    successLabel.setVisible(true);
  }

  @Override
  public void uploadFailed(Exception exception) {
    errorLabel.setVisible(true);
    LOGGER.error("Failed to import ldif-file.", exception);
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

  }

}
