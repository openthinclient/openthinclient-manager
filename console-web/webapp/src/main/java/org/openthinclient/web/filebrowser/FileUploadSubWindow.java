package org.openthinclient.web.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.server.Page;
import com.vaadin.ui.*;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class FileUploadSubWindow extends Window {

   /** serialVersionUID */
   private static final long serialVersionUID = 641733612432869212L;
   
   private MyReceiver receiver = new MyReceiver();
   private Path doc;
   private Label fileUploadInfoLabel;

   private FileBrowserView fileBrowserView;
   private IMessageConveyor mc;

   public FileUploadSubWindow(FileBrowserView fileBrowserView, Path doc, Path managerHomePath) {
      
      this.fileBrowserView = fileBrowserView;
      
      addCloseListener(event -> {
         UI.getCurrent().removeWindow(this);
      });
      
      mc = new MessageConveyor(UI.getCurrent().getLocale());

      if (doc == null) {
         this.doc = managerHomePath;
      } else if (Files.isDirectory(doc)) {
         this.doc = doc;
      } else {
         this.doc = doc.getParent();
      }
      setCaption(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_UPLOAD_CAPTION, this.doc.getFileName()));
      setHeight("140px");
      setWidth("500px");
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setSizeFull();
      setContent(subContent);

      Upload upload = new Upload(null, receiver);
      upload.addSucceededListener(receiver);
      upload.setImmediateMode(true);
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
              new Notification(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_UPLOAD_FAIL), Notification.Type.ERROR_MESSAGE).show(Page.getCurrent());
              return null;
          }
          return fos; // Return the output stream to write to
      }

      @Override
      public void uploadSucceeded(SucceededEvent event) {
         fileUploadInfoLabel.setValue(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_UPLOAD_SUCCESS, file.getFileName()));
         fileUploadInfoLabel.setEnabled(true);
         fileBrowserView.refresh(file);
      }
  }   
}
