package org.openthinclient.web.filebrowser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;

import javax.annotation.PostConstruct;

import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.FilesystemContainer;
import com.vaadin.data.util.TextFileProperty;
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
   
   private MyReceiver receiver = new MyReceiver();
   private Upload upload = new Upload(null, receiver);
   
   private File selectedFileItem;

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

      LOGGER.debug("Manageing files from ", managerHome.getLocation());
      
      VerticalLayout verticalLayout = new VerticalLayout();
      verticalLayout.setSpacing(true);
      
      HorizontalLayout controlBar = new HorizontalLayout();
      controlBar.setSpacing(true);
      
      Button contentButton = new Button("Show Content", new Button.ClickListener() {
         @Override
         public void buttonClick(ClickEvent event) {
            FileContentSubWindow sub = new FileContentSubWindow(selectedFileItem);
            // Add it to the root component
            UI.getCurrent().addWindow(sub);
         }
      });
      contentButton.setEnabled(false);
      contentButton.setIcon(FontAwesome.EYE);
      contentButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(contentButton);
      
      
      Button createDirButton = new Button("Create Directory", new Button.ClickListener() {
         @Override
         public void buttonClick(ClickEvent event) {
            // TODO Auto-generated method stub
         }
      });
      createDirButton.setIcon(FontAwesome.FOLDER_O);
      createDirButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(createDirButton);
      
      Button downloadButton = new Button("Download", new Button.ClickListener() {
         @Override
         public void buttonClick(ClickEvent event) {
            // TODO Auto-generated method stub
         }
      });
      downloadButton.setIcon(FontAwesome.DOWNLOAD);
      downloadButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
      controlBar.addComponent(downloadButton);
      
//      Button uploadButton = new Button("Uplaod", new Button.ClickListener() {
//         @Override
//         public void buttonClick(ClickEvent event) {
//            // TODO Auto-generated method stub
//         }
//      });
//      uploadButton.setIcon(FontAwesome.UPLOAD);
//      uploadButton.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
//      controlBar.addComponent(uploadButton);
//      
      
      verticalLayout.addComponent(controlBar);
      
      FilesystemContainer docs = new FilesystemContainer(managerHome.getLocation(), false);
      TreeTable docList = new TreeTable(null, docs);
      docList.setStyleName(ValoTheme.TREETABLE_COMPACT);
      docList.setItemIconPropertyId("Icon");
      docList.setVisibleColumns(new Object[]{"Name", "Size", "Last Modified"});
      docList.setImmediate(true);
      docList.setSelectable(true);       
      docList.setSizeFull();
      verticalLayout.addComponent(docList);
      
      docList.addValueChangeListener(new Property.ValueChangeListener() {
         @Override
         public void valueChange(ValueChangeEvent event) {
            selectedFileItem = (File) event.getProperty().getValue();
            contentButton.setEnabled(selectedFileItem != null);
         }
      });
      
      return verticalLayout;
  }

   @Override
   public void enter(ViewChangeEvent event) {
      
   }
 
   public class MyReceiver implements Receiver, SucceededListener {

      /** serialVersionUID */
      private static final long serialVersionUID = -5844542658116931976L;
      private final transient Logger LOGGER = LoggerFactory.getLogger(MyReceiver.class);

      private String filename;
      private String mimetype;
      private byte[] data;

      ByteArrayOutputStream fos = null; // Stream to write to

      public OutputStream receiveUpload(String filename, String mimetype) {
          this.filename = filename;
          this.mimetype = mimetype;
          LOGGER.debug("Receive file {} and type {}", filename, mimetype);
          return new ByteArrayOutputStream() {
              @Override
              public void close() {
                  data = toByteArray();
              }

          }; // Return the output stream to write to
      }

      @Override
      public void uploadSucceeded(SucceededEvent event) {
          Notification.show("File received");
      }

      /**
       * Returns the value of data.
       *
       * @return value of data
       */
      public byte[] getData() {
          return data;
      }

      /**
       * Set the value of data to data.
       *
       * @param data new value of data
       */
      public void setData(byte[] data) {
          this.data = data;
      }

      /**
       * Returns the value of filename.
       *
       * @return value of filename
       */
      public String getFilename() {
          return filename;
      }

      /**
       * Set the value of filename to filename.
       *
       * @param filename new value of filename
       */
      public void setFilename(String filename) {
          this.filename = filename;
      }

      /**
       * Returns the value of mimetype.
       *
       * @return value of mimetype
       */
      public String getMimetype() {
          return mimetype;
      }

      /**
       * Set the value of mimetype to mimetype.
       *
       * @param mimetype new value of mimetype
       */
      public void setMimetype(String mimetype) {
          this.mimetype = mimetype;
      }

  }   
}
