package org.openthinclient.web.support;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.api.ldif.export.LdifExporterService;
import org.openthinclient.api.ldif.export.LdifImporterService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.filebrowser.FileUploadSubWindow;
import org.openthinclient.web.filebrowser.FileUploadView;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.SettingsUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.icons.VaadinIcons;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_LDIF_IMPORT_EXPORT_HEADER;

@SpringView(name = "ldif-import-export", ui = SettingsUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SUPPORT_LDIF_IMPORT_EXPORT_HEADER", order = 71)
public class LdifImportExportView extends Panel implements View, FileUploadView {

  private static final Logger LOGGER = LoggerFactory.getLogger(LdifImportExportView.class);

  @Autowired
  protected ManagerHome managerHome;
  @Autowired
  private RealmService realmService;

  private EventBus.SessionEventBus eventBus;
  private MyReceiver receiver = new MyReceiver();
  final MessageConveyor mc;
  final VerticalLayout root ;

  private Label importSuccessLabel;
  private Label importErrorLabel;
  private Label exportErrorLabel;
  private Label exportSuccessLabel;

  Set<LdifImporterService.State> importResult = new HashSet<>();
  Set<LdifExporterService.State> exportResult = new HashSet<>();

  public LdifImportExportView(EventBus.SessionEventBus eventBus) {

    this.eventBus = eventBus;

    mc = new MessageConveyor(UI.getCurrent().getLocale());
    setSizeFull();

    root = new VerticalLayout();
    root.setSizeFull();
    root.setMargin(true);
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

    VerticalLayout content = new VerticalLayout();

    // Import
    final Label labelImportDescription = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LDIF_IMPORT_DESCRIPTION), ContentMode.HTML);
    content.addComponent(labelImportDescription);
    HorizontalLayout importHL = new HorizontalLayout();
    content.addComponent(importHL);
    importSuccessLabel = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LDIF_IMPORT_SUCCESS), ContentMode.HTML);
    importSuccessLabel.setVisible(false);
    importErrorLabel = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LDIF_IMPORT_FAILED), ContentMode.HTML);
    importErrorLabel.setVisible(false);
    importErrorLabel.setStyleName("unexpected_error");

    Upload upload = new Upload(null, receiver);
    upload.addStyleName("ldif-import");
    upload.setButtonCaption("LDIF Import");
    upload.setAcceptMimeTypes("text/ldif");
    upload.addSucceededListener(receiver);
    upload.setImmediateMode(true);
    upload.addStartedListener(e -> {
      importSuccessLabel.setVisible(false);
      importErrorLabel.setVisible(false);
    });
    importHL.addComponents(upload, importSuccessLabel, importErrorLabel);

//    Button uploadButton = new Button("LDIF Import" /*, new ThemeResource(icon)*/);
//    uploadButton.addClickListener(e -> {
//          importSuccessLabel.setVisible(false);
//          importErrorLabel.setVisible(false);
//        }
//    );
//    uploadButton.addStyleName("thinclient-action-button");
//    uploadButton.setIcon(VaadinIcons.UPLOAD);
////    uploadButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
//    content.addComponent(uploadButton);

    content.addComponent(new Label("<hr/>",ContentMode.HTML));

    // Export
    final Label labelExportDescription = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LDIF_EXPORT_DESCRIPTION), ContentMode.HTML);
    content.addComponent(labelExportDescription);

    HorizontalLayout exportHL = new HorizontalLayout();
    content.addComponent(exportHL);
    exportSuccessLabel = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LDIF_EXPORT_SUCCESS), ContentMode.HTML);
    exportSuccessLabel.setVisible(false);
    exportErrorLabel = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_LDIF_EXPORT_FAILED), ContentMode.HTML);
    exportErrorLabel.setVisible(false);
    exportErrorLabel.setStyleName("unexpected_error");
    Button exportButton = new Button("LDIF Export");
    exportButton.addStyleName("ldif-export");
    // attach file-downloader
    FileDownloader fileDownloader = new FileDownloader(createResource());
    fileDownloader.extend(exportButton);
    exportHL.addComponents(exportButton, exportSuccessLabel, exportErrorLabel);


    root.addComponent(content);
    root.setExpandRatio(content, 1);
  }


  private StreamResource createResource() {
    return new StreamResource((StreamResource.StreamSource) () -> {
      LdifExporterService ldifExporterService = new LdifExporterService(realmService.getDefaultRealm().getConnectionDescriptor());
      byte[] bytes = ldifExporterService.performAction(Collections.singleton(""), exportResult::add);
      if (exportResult.contains(LdifExporterService.State.ERROR) || exportResult.contains(LdifExporterService.State.EXCEPTION)) {
        exportErrorLabel.setVisible(true); // will be shown only on a UI-click (..somewhere)
        return null;
      } else {
        exportSuccessLabel.setVisible(true);
        return new ByteArrayInputStream(bytes);
      }
    }, "openthinclient-export.ldif");
  }

  public void uploadSucceed(Path file) {
    LdifImporterService lis = new LdifImporterService(realmService.getDefaultRealm().getConnectionDescriptor());
    try {
      lis.importTempFile(file.toFile(), importResult::add);
      // check import-result
      if (importResult.contains(LdifImporterService.State.ERROR) || importResult.contains(LdifImporterService.State.EXCEPTION)) {
        importErrorLabel.setVisible(true);
      } else {
        importSuccessLabel.setVisible(true);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to import file " + file.getFileName(), e);
      importErrorLabel.setVisible(true);
    }
    eventBus.publish(this, new DashboardEvent.ClientCountChangeEvent());
  }

  @Override
  public void uploadFailed(Exception exception) {
    importErrorLabel.setVisible(true);
    LOGGER.error("Failed to import ldif-file.", exception);
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

  }

  /**
   * The file upload receiver.
   */
  public class MyReceiver implements Upload.Receiver, Upload.SucceededListener, Upload.FailedListener {

    /** serialVersionUID */
    private static final long serialVersionUID = -5844542658116931976L;
    private final transient Logger LOGGER = LoggerFactory.getLogger(FileUploadSubWindow.MyReceiver.class);

    public Path file;

    public OutputStream receiveUpload(String filename, String mimeType) {
      // Create upload stream
      FileOutputStream fos = null; // Stream to write to
      try {
        // Open the file for writing.
        // TODO path
        file = managerHome.getLocation().toPath().resolve(filename);
        fos = new FileOutputStream(file.toFile());
      } catch (final java.io.FileNotFoundException e) {
        LOGGER.error("Could not open file", e);
        new Notification(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_UPLOAD_FAIL), Notification.Type.ERROR_MESSAGE).show(Page.getCurrent());
        return null;
      }
      return fos; // Return the output stream to write to
    }

    @Override
    public void uploadSucceeded(Upload.SucceededEvent event) {
      LdifImportExportView.this.uploadSucceed(file);
    }

    @Override
    public void uploadFailed(Upload.FailedEvent failedEvent) {
      // TODO show Error
    }
  }

}
