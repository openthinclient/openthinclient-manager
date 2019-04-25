package org.openthinclient.flow.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import org.openthinclient.flow.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class FileUploadSubWindow extends Dialog {

   /** serialVersionUID */
   private static final long serialVersionUID = 641733612432869212L;
   
   private MyReceiver receiver = new MyReceiver();
   private Path doc;
   private Label fileUploadInfoLabel;

   private FileBrowserView fileBrowserView;
   private IMessageConveyor mc;

   public FileUploadSubWindow(FileBrowserView fileBrowserView, Path doc, Path managerHomePath) {
      
      this.fileBrowserView = fileBrowserView;
      
      addDialogCloseActionListener(event -> {
         UI.getCurrent().remove(this);
      });
      
      mc = new MessageConveyor(UI.getCurrent().getLocale());

      if (doc == null) {
         this.doc = managerHomePath;
      } else if (Files.isDirectory(doc)) {
         this.doc = doc;
      } else {
         this.doc = doc.getParent();
      }
      add(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_UPLOAD_CAPTION, this.doc.getFileName()));
      setHeight("140px");
      setWidth("500px");

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setSizeFull();
      add(subContent);

      FileBuffer fileBuffer = new FileBuffer();
      Upload upload1 = new Upload(receiver);
      upload1.setReceiver(receiver);
      upload1.setAutoUpload(true);
      upload1.addFailedListener(e -> {
         new Notification(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_UPLOAD_FAIL));
      });
      upload1.addSucceededListener(e -> {
//         InputStream inputStream = fileBuffer.getInputStream();
//         try {
            Path file = FileUploadSubWindow.this.doc.resolve(e.getFileName());
//            FileUtils.copyInputStreamToFile(inputStream, file.toFile());

            fileUploadInfoLabel.setTitle(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_UPLOAD_SUCCESS, file.getFileName()));
            fileUploadInfoLabel.setEnabled(true);
            fileBrowserView.refresh(file);

//         } catch (IOException e1) {
//            e1.printStackTrace();
//            new Notification(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_UPLOAD_FAIL));
//         }
      });
      subContent.add(upload1);

      fileUploadInfoLabel = new Label();
      fileUploadInfoLabel.setEnabled(false);
      subContent.add(fileUploadInfoLabel);
      
   }
   
   /**
    * The file upload receiver.
    */
   public class MyReceiver implements Receiver {

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
//              new Notification(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_UPLOAD_FAIL), Notification.Type.ERROR_MESSAGE).show(Page.getCurrent());
              return null;
          }
          return fos; // Return the output stream to write to
      }

  }
}
