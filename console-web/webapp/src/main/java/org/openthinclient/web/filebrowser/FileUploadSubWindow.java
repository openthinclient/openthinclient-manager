package org.openthinclient.web.filebrowser;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.Page;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


public class FileUploadSubWindow extends Window {

   /** serialVersionUID */
   private static final long serialVersionUID = 641733612432869212L;
   
   private MyReceiver receiver = new MyReceiver();
   private Path doc;
   private Label fileUploadInfoLabel;

   private FileBrowserView fileBrowserView;

   public FileUploadSubWindow(FileBrowserView fileBrowserView, Path doc) {
      
      this.fileBrowserView = fileBrowserView;
      
      addCloseListener(event -> {
         UI.getCurrent().removeWindow(this);
      });
      
      setWidth("500px");
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setHeight("100%");
      setContent(subContent);
      
      if (Files.isDirectory(doc)) {
         this.doc = doc;
      } else {
         this.doc = doc.getParent();
      }
      setCaption("Upload to " + this.doc.getFileName());
      setHeight("140px");

      Upload upload = new Upload(null, receiver);
      upload.addSucceededListener(receiver);
      upload.setImmediate(true);
      subContent.addComponent(upload);
      
      fileUploadInfoLabel = new Label();
      fileUploadInfoLabel.setEnabled(false);
      subContent.addComponent(fileUploadInfoLabel);
      
   }
   
   /**
    * The file upload receiver.
    */
   public class MyReceiver implements Receiver, SucceededListener {

      /** serialVersionUID */
      private static final long serialVersionUID = -5844542658116931976L;
      private final transient Logger LOGGER = LoggerFactory.getLogger(MyReceiver.class);

      public Path file;

      public OutputStream receiveUpload(String filename, String mimeType) {
          // Create upload stream
          FileOutputStream fos = null; // Stream to write to
          try {
              // Open the file for writing.
              file = FileUploadSubWindow.this.doc.resolve(filename);
              fos = new FileOutputStream(file.toFile());
          } catch (final java.io.FileNotFoundException e) {
              LOGGER.error("Could not open file", e);
              new Notification("Could not open file<br/>", e.getMessage(), Notification.Type.ERROR_MESSAGE).show(Page.getCurrent());
              return null;
          }
          return fos; // Return the output stream to write to
      }

      @Override
      public void uploadSucceeded(SucceededEvent event) {
         fileUploadInfoLabel.setValue("The fileupload to " + file.getFileName() + " succeed.");
         fileUploadInfoLabel.setEnabled(true);
         fileBrowserView.refresh();
      }
  }   
}
