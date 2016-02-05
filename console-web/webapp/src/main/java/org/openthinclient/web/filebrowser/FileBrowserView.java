package org.openthinclient.web.filebrowser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;


import javax.annotation.PostConstruct;


import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.filebrowser.FileContentSubWindow.WindowType;
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
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.FileTypeResolver;

@SuppressWarnings("serial")
@SpringView(name = "filebrowser")
@SideBarItem(sectionId = DashboardSections.COMMON, caption = "Filebrowser", order=99)
public final class FileBrowserView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserView.class);
   
   @Autowired
   private EventBus.SessionEventBus eventBus;
   @Autowired
   private ManagerHome managerHome;

   private final VerticalLayout root;
   
   private File selectedFileItem;
   private Button contentButton;
   private Button createDirButton;
   private Button downloadButton;
   
   private FileContentSubWindow fcSubWindow = null;
   private CreateDirectorySubWindow createDirectorySubWindow = null;

   private Button uploadButton;

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
      
      VerticalLayout verticalLayout = new VerticalLayout();
      verticalLayout.setSpacing(true);
      
      HorizontalLayout controlBar = new HorizontalLayout();
      controlBar.setSpacing(true);

      this.contentButton = new Button("Show Content", event -> {
         if (fcSubWindow != null) {
            fcSubWindow.close();
            UI.getCurrent().removeWindow(fcSubWindow);
            fcSubWindow = null;
         } else {
            fcSubWindow = new FileContentSubWindow(WindowType.CONTENT, selectedFileItem);
            UI.getCurrent().addWindow(fcSubWindow);
         }
      });
      this.contentButton.setEnabled(false);
      this.contentButton.setIcon(FontAwesome.EYE);
      this.contentButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(this.contentButton);

      this.createDirButton = new Button("Create Directory", event -> {
         if (createDirectorySubWindow != null) {
            createDirectorySubWindow.close();
            UI.getCurrent().removeWindow(createDirectorySubWindow);
            createDirectorySubWindow = null;
         } else {
            createDirectorySubWindow = new CreateDirectorySubWindow(selectedFileItem);
            UI.getCurrent().addWindow(createDirectorySubWindow);
         }
      });
      this.createDirButton.setIcon(FontAwesome.FOLDER_O);
      this.createDirButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(this.createDirButton);

      
      
      this.downloadButton = new Button("Download", event -> {
         // TODO Auto-generated method stub
      });
      this.downloadButton.setIcon(FontAwesome.DOWNLOAD);
      this.downloadButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(this.downloadButton);
      
      uploadButton = new Button("Upload", event -> {
         if (fcSubWindow != null) {
            fcSubWindow.close();
            UI.getCurrent().removeWindow(fcSubWindow);
            fcSubWindow = null;
         } else {
            fcSubWindow = new FileContentSubWindow(WindowType.UPLOAD, selectedFileItem);
            UI.getCurrent().addWindow(fcSubWindow);
         }
      });
      uploadButton.setIcon(FontAwesome.UPLOAD);
      uploadButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(uploadButton);
      
      
      verticalLayout.addComponent(controlBar);
      
      File rootLocation = managerHome.getLocation();
      selectedFileItem = rootLocation;
      
      FilesystemContainer docs = new FilesystemContainer(rootLocation, false);      
      TreeTable docList = new TreeTable(null, docs);
      docList.setStyleName(ValoTheme.TREETABLE_COMPACT);
      docList.setItemIconPropertyId("Icon");
      docList.setVisibleColumns("Name", "Size", "Last Modified");
      docList.setImmediate(true);
      docList.setSelectable(true);       
      docList.setSizeFull();
      verticalLayout.addComponent(docList);
      
      docList.addValueChangeListener(event -> {
         onSelectedFileItemChanged((File)event.getProperty().getValue());
      });
      
      return verticalLayout;
  }

   private void onSelectedFileItemChanged(File value) {
      selectedFileItem = value;
      contentButton.setEnabled(selectedFileItem != null);
      uploadButton.setEnabled(uploadButton != null);
      downloadButton.setEnabled(selectedFileItem != null);
      contentButton.setEnabled(selectedFileItem != null && FileContentSubWindow.isMimeTypeSupported(FileTypeResolver.getMIMEType(selectedFileItem)));
      createDirButton.setEnabled(selectedFileItem != null && selectedFileItem.isDirectory());
   }

   @Override
   public void enter(ViewChangeEvent event) {
      
   }
 
  
}
