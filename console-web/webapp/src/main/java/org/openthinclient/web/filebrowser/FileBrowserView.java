package org.openthinclient.web.filebrowser;

import java.io.File;
import java.util.ArrayList;

import javax.annotation.PostConstruct;

import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.filebrowser.FileBrowserSubWindow.WindowType;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import com.vaadin.data.util.FilesystemContainer;
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
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.FileTypeResolver;

@SuppressWarnings("serial")
@SpringView(name = "filebrowser")
@SideBarItem(sectionId = DashboardSections.COMMON, caption = "Filebrowser", order = 99)
public final class FileBrowserView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserView.class);

   @Autowired
   private EventBus.SessionEventBus eventBus;
   @Autowired
   private ManagerHome managerHome;

   private final VerticalLayout root;
   private VerticalLayout content;

   private Button removeDirButton;
   private File selectedFileItem;
   private Button contentButton;
   private Button createDirButton;
   private Button downloadButton;
   private Button uploadButton;
   private TreeTable docList;


   public FileBrowserView() {

      addStyleName(ValoTheme.PANEL_BORDERLESS);
      setSizeFull();
      DashboardEventBus.register(this);

      root = new VerticalLayout();
      root.setSizeFull();
      root.setMargin(true);
      root.addStyleName("dashboard-view");
      setContent(root);
      Responsive.makeResponsive(root);

      root.addComponent(new ViewHeader("Filebrowser"));
      root.addComponent(buildSparklines());

   }

   @PostConstruct
   private void init() {
      Component content = buildContent();
      root.addComponent(content);
      root.setExpandRatio(content, 1);
   }

   private Component buildSparklines() {
      CssLayout sparks = new CssLayout();
      sparks.addStyleName("sparks");
      sparks.setWidth("100%");
      Responsive.makeResponsive(sparks);

      return sparks;
   }

   private Component buildContent() {

      LOGGER.debug("Managing files from ", managerHome.getLocation());
      this.selectedFileItem = managerHome.getLocation();
      
      content = new VerticalLayout();
      content.setSpacing(true);

      HorizontalLayout controlBar = new HorizontalLayout();
      controlBar.setSpacing(true);

      this.contentButton = new Button("Show Content", event -> {
         showSubWindow(WindowType.CONTENT);
      });
      this.contentButton.setEnabled(false);
      this.contentButton.setIcon(FontAwesome.EYE);
      this.contentButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(this.contentButton);

      // Create directory
      this.createDirButton = new Button("Create Directory", event -> {
         showSubWindow(WindowType.CREATE_DIRECTORY);
      });
      this.createDirButton.setIcon(FontAwesome.FOLDER_O);
      this.createDirButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(this.createDirButton);

      this.removeDirButton = new Button("Remove Directory", event -> {
         showSubWindow(WindowType.REMOVE);
      });
      this.removeDirButton.setIcon(FontAwesome.TIMES);
      this.removeDirButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      this.removeDirButton.setEnabled(false);
      controlBar.addComponent(removeDirButton);

      // Upload/Download group
      CssLayout groupUploadDownload = new CssLayout();
      groupUploadDownload.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      this.downloadButton = new Button("Download");
      this.downloadButton.setIcon(FontAwesome.DOWNLOAD);
      this.downloadButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      this.downloadButton.setEnabled(false);
      groupUploadDownload.addComponent(this.downloadButton);

      uploadButton = new Button("Upload", event -> {
         showSubWindow(WindowType.UPLOAD);
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

   private void showSubWindow(WindowType windowType) {
      UI.getCurrent().addWindow(new FileBrowserSubWindow(this, windowType, selectedFileItem));
   }

   private void createTreeTable() {
      FilesystemContainer docs = new FilesystemContainer(managerHome.getLocation(), false);
      docList = new TreeTable(null, docs);
      docList.setStyleName(ValoTheme.TREETABLE_COMPACT);
      docList.setItemIconPropertyId("Icon");
      docList.setVisibleColumns("Name", "Size", "Last Modified");
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
      selectedFileItem = value;
      contentButton.setEnabled(selectedFileItem != null);
      uploadButton.setEnabled(uploadButton != null);
      contentButton.setEnabled(selectedFileItem != null && FileBrowserSubWindow.isMimeTypeSupported(FileTypeResolver.getMIMEType(selectedFileItem)));
      removeDirButton.setEnabled(uploadButton != null);

      if (selectedFileItem != null && selectedFileItem.isFile()) {
         downloadButton.setEnabled(true);
         // Remove FileDownload-extensions on button-object
         new ArrayList<Extension>(downloadButton.getExtensions()).forEach(ex -> downloadButton.removeExtension(ex));
         FileDownloader fileDownloader = new FileDownloader(new FileResource(selectedFileItem));
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
