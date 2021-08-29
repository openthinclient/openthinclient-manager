package org.openthinclient.web.support;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import org.openthinclient.api.ldif.export.LdifExporterService;
import org.openthinclient.api.ldif.export.LdifImporterService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEvent;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_LDIF_EXPORT_DESCRIPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_LDIF_EXPORT_FAILED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_LDIF_EXPORT_SUCCESS;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_LDIF_IMPORT_DESCRIPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_LDIF_IMPORT_EXPORT_HEADER;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_LDIF_IMPORT_FAILED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_LDIF_IMPORT_SUCCESS;

@SpringView(name = "ldif-import-export", ui = SettingsUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SUPPORT_LDIF_IMPORT_EXPORT_HEADER", order = 71)
public class LdifImportExportView extends Panel implements View {

  private static final Logger LOGGER = LoggerFactory.getLogger(LdifImportExportView.class);

  @Autowired
  protected ManagerHome managerHome;
  @Autowired
  private RealmService realmService;

  private EventBus.SessionEventBus eventBus;
  final MessageConveyor mc;
  final CssLayout root;

  private File file;

  private Label importSuccessLabel;
  private Label importErrorLabel;
  private Label exportErrorLabel;
  private Label exportSuccessLabel;

  public LdifImportExportView(EventBus.SessionEventBus eventBus) {

    this.eventBus = eventBus;

    mc = new MessageConveyor(UI.getCurrent().getLocale());
    setSizeFull();

    root = new CssLayout();
    root.setStyleName("importexport");
    setContent(root);
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

    // Export
    exportSuccessLabel = buildFeedbackLabel(UI_SUPPORT_LDIF_EXPORT_SUCCESS, "success");
    exportErrorLabel = buildFeedbackLabel(UI_SUPPORT_LDIF_EXPORT_FAILED, "failure");
    root.addComponent(new CssLayout(
      new CssLayout(
        buildExportButton(),
        exportSuccessLabel,
        exportErrorLabel
      ),
      new Label(mc.getMessage(UI_SUPPORT_LDIF_EXPORT_DESCRIPTION), ContentMode.HTML)
    ));

    // Import
    importSuccessLabel = buildFeedbackLabel(UI_SUPPORT_LDIF_IMPORT_SUCCESS, "success");
    importErrorLabel = buildFeedbackLabel(UI_SUPPORT_LDIF_IMPORT_FAILED, "failure");
    root.addComponent(new CssLayout(
      new CssLayout(
        buildUploadButton(),
        importSuccessLabel,
        importErrorLabel
      ),
      new Label(mc.getMessage(UI_SUPPORT_LDIF_IMPORT_DESCRIPTION), ContentMode.HTML)
    ));

  }

  private Label buildFeedbackLabel(ConsoleWebMessages msg, String... styleNames) {
    Label label = new Label(mc.getMessage(msg), ContentMode.HTML);
    label.setVisible(false);
    label.addStyleNames(styleNames);
    return label;
  }

  private Button buildExportButton() {
    Button exportButton = new Button("LDIF Export");
    exportButton.addStyleName("ldif-export");
    FileDownloader fileDownloader = new FileDownloader(createDownloadResource());
    fileDownloader.extend(exportButton);
    return exportButton;
  }

  private Upload buildUploadButton() {
    Upload upload = new Upload(null, (filename, mimeType) -> {
      try {
        file = File.createTempFile(filename, "ldif", managerHome.getLocation());
        return new FileOutputStream(file);
      } catch (final IOException e) {
        LOGGER.error("Could not open file", e);
        return null;
      }
    });
    upload.addStyleName("ldif-import");
    upload.setButtonCaption("LDIF Import");
    upload.setAcceptMimeTypes("text/ldif");
    upload.addSucceededListener(ev -> this.importLdifFile(file));
    upload.addFailedListener(ev -> this.uploadFailed(ev.getReason()));
    upload.setImmediateMode(true);
    upload.addStartedListener(e -> {
      importSuccessLabel.setVisible(false);
      importErrorLabel.setVisible(false);
      getUI().access(() -> getUI().push());
    });
    return upload;
  }

  private StreamResource createDownloadResource() {
    return new StreamResource((StreamResource.StreamSource) () -> {
      exportErrorLabel.setVisible(false);
      exportSuccessLabel.setVisible(false);
      getUI().access(() -> getUI().push());
      LdifExporterService ldifExporterService = new LdifExporterService(realmService.getDefaultRealm().getConnectionDescriptor());
      Set<LdifExporterService.State> exportResult = new HashSet<>();
      byte[] bytes = ldifExporterService.performAction(Collections.singleton(""), exportResult::add);
      if (exportResult.contains(LdifExporterService.State.ERROR) || exportResult.contains(LdifExporterService.State.EXCEPTION)) {
        exportErrorLabel.setVisible(true);
        getUI().access(() -> getUI().push());
        return null;
      } else {
        exportSuccessLabel.setVisible(true);
        getUI().access(() -> getUI().push());
        return new ByteArrayInputStream(bytes);
      }
    }, "openthinclient-export.ldif");
  }

  private void importLdifFile(File file) {
    LdifImporterService lis = new LdifImporterService(realmService.getDefaultRealm().getConnectionDescriptor());
    Set<LdifImporterService.State> importResult = new HashSet<>();
    try {
      lis.importTempFile(file, importResult::add);
      if (importResult.contains(LdifImporterService.State.ERROR) || importResult.contains(LdifImporterService.State.EXCEPTION)) {
        importErrorLabel.setVisible(true);
      } else {
        importSuccessLabel.setVisible(true);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to import file " + file.getName(), e);
      importErrorLabel.setVisible(true);
    }
    getUI().access(() -> getUI().push());
    eventBus.publish(this, new DashboardEvent.LDAPImportEvent());
    file.delete();
  }

  private void uploadFailed(Exception exception) {
    LOGGER.error("Failed to import LDIF-file.", exception);
    importErrorLabel.setVisible(true);
    getUI().access(() -> getUI().push());
  }

}
