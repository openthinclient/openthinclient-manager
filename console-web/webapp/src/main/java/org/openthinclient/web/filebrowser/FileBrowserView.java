package org.openthinclient.web.filebrowser;

import com.vaadin.v7.data.util.FilesystemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Extension;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.v7.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.FileTypeResolver;

import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.ui.Sparklines;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.annotation.PostConstruct;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_BUTTON_DOWNLOAD;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_BUTTON_MKDIR;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_BUTTON_RMDIR;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_BUTTON_UPLOAD;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_BUTTON_VIEWCONTENT;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_COLUMN_MODIFIED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_COLUMN_NAME;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_COLUMN_SIZE;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_FILEBROWSER_HEADER;

@SuppressWarnings("serial")
@SpringView(name = "filebrowser")
@SideBarItem(sectionId = DashboardSections.COMMON, captionCode="UI_FILEBROWSER_HEADER", order = 99)
public final class FileBrowserView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserView.class);
   final IMessageConveyor mc;
   private final VerticalLayout root;
   @Autowired
   private EventBus.SessionEventBus eventBus;
   @Autowired
   private ManagerHome managerHome;
   private VerticalLayout content;
   private Button removeDirButton;
   private Path selectedFileItem;
   private Button contentButton;
   private Button createDirButton;
   private Button downloadButton;
   private Button uploadButton;
   private TreeTable docList;
   private Window subWindow;

   public FileBrowserView() {

      mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addStyleName(ValoTheme.PANEL_BORDERLESS);
      setSizeFull();
      DashboardEventBus.register(this);

      root = new VerticalLayout();
      root.setSizeFull();
      root.setMargin(true);
      root.addStyleName("dashboard-view");
      setContent(root);
      Responsive.makeResponsive(root);

      root.addComponent(new ViewHeader(mc.getMessage(UI_FILEBROWSER_HEADER)));

   }

   public static boolean isMimeTypeSupported(String mimeType) {
      switch (mimeType) {
         case "text/plain":
         case "text/xml":
         case "text/html":
         case "image/png":
         case "image/gif":
         case "image/jpg":
         case "image/jpeg":
            return true;
      }
      return false;
   }

   @Override
   public String getCaption() {
      return mc.getMessage(UI_FILEBROWSER_HEADER);
   }

   @PostConstruct
   private void init() {
      Component content = buildContent();
      root.addComponent(content);
      root.setExpandRatio(content, 1);
   }

   private Component buildContent() {

      LOGGER.debug("Managing files from ", managerHome.getLocation());
      this.selectedFileItem = managerHome.getLocation().toPath();

      content = new VerticalLayout();
      content.setSpacing(true);

      HorizontalLayout controlBar = new HorizontalLayout();
      controlBar.setSpacing(true);

      this.contentButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_VIEWCONTENT), event -> {
         showSubwindow(new ContentViewSubWindow(this, selectedFileItem));
      });
      this.contentButton.setEnabled(false);
      this.contentButton.setIcon(FontAwesome.EYE);
      this.contentButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(this.contentButton);

      // Create directory
      this.createDirButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_MKDIR), event -> {
         showSubwindow(new CreateDirectorySubWindow(this, selectedFileItem));
      });
      this.createDirButton.setIcon(FontAwesome.FOLDER_O);
      this.createDirButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(this.createDirButton);

      this.removeDirButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_RMDIR), event -> {
         showSubwindow(new RemoveItemSubWindow(this, selectedFileItem));
      });
      this.removeDirButton.setIcon(FontAwesome.TIMES);
      this.removeDirButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      this.removeDirButton.setEnabled(false);
      controlBar.addComponent(removeDirButton);

      // Upload/Download group
      CssLayout groupUploadDownload = new CssLayout();
      groupUploadDownload.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      this.downloadButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_DOWNLOAD));
      this.downloadButton.setIcon(FontAwesome.DOWNLOAD);
      this.downloadButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      this.downloadButton.setEnabled(false);
      groupUploadDownload.addComponent(this.downloadButton);

      uploadButton = new Button(mc.getMessage(UI_FILEBROWSER_BUTTON_UPLOAD), event -> {
         showSubwindow(new FileUploadSubWindow(this, selectedFileItem));
      });
      uploadButton.setIcon(FontAwesome.UPLOAD);
      uploadButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      groupUploadDownload.addComponent(uploadButton);
      controlBar.addComponent(groupUploadDownload);

      content.addComponent(controlBar);
      createTreeTable();
      content.addComponent(docList);

      return content;
   }

   private void showSubwindow(Window windowToShow) {
      UI.getCurrent().removeWindow(subWindow);
      UI.getCurrent().addWindow(subWindow = windowToShow);
   }

   private void createTreeTable() {
      FilesystemContainer docs = new FilesystemContainer(managerHome.getLocation(), false);
      docList = new TreeTable(null, docs);
      docList.setStyleName(ValoTheme.TREETABLE_COMPACT);
      docList.setItemIconPropertyId("Icon");
      docList.setVisibleColumns("Name", "Size", "Last Modified");
      // Set nicer header names
      docList.setColumnHeader(FilesystemContainer.PROPERTY_NAME, mc.getMessage(UI_FILEBROWSER_COLUMN_NAME));
      docList.setColumnHeader(FilesystemContainer.PROPERTY_SIZE, mc.getMessage(UI_FILEBROWSER_COLUMN_SIZE));
      docList.setColumnHeader(FilesystemContainer.PROPERTY_LASTMODIFIED, mc.getMessage(UI_FILEBROWSER_COLUMN_MODIFIED));

      docList.setImmediate(true);
      docList.setSelectable(true);
      docList.setSizeFull();
      docList.addValueChangeListener(event -> {
         onSelectedFileItemChanged((File) event.getProperty().getValue());
      });
   }

   public void refresh() {
      content.removeComponent(docList);
      createTreeTable();
      content.addComponent(docList);
   }

   private void onSelectedFileItemChanged(File value) {
      selectedFileItem = value.toPath();
      contentButton.setEnabled(selectedFileItem != null);
      uploadButton.setEnabled(uploadButton != null);
      contentButton.setEnabled(selectedFileItem != null && isMimeTypeSupported(FileTypeResolver.getMIMEType(selectedFileItem.toFile())));
      removeDirButton.setEnabled(uploadButton != null);

      if (selectedFileItem != null && Files.isRegularFile(selectedFileItem)) {
         downloadButton.setEnabled(true);
         // Remove FileDownload-extensions on button-object
         new ArrayList<Extension>(downloadButton.getExtensions()).forEach(ex -> downloadButton.removeExtension(ex));
         FileDownloader fileDownloader = new FileDownloader(new FileResource(selectedFileItem.toFile()));
         fileDownloader.setOverrideContentType(false);
         fileDownloader.extend(downloadButton);
      } else {
         downloadButton.setEnabled(false);
      }

   }
      
   @Override
   public void enter(ViewChangeEvent event) {

   }

}
