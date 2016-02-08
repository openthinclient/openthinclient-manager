package org.openthinclient.web.filebrowser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeTypeUtils;

import com.vaadin.data.util.TextFileProperty;
import com.vaadin.server.FileResource;
import com.vaadin.server.Page;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.util.FileTypeResolver;

public class FileContentSubWindow extends Window {

   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 3887697184057926390L;

   private MyReceiver receiver = new MyReceiver();
   private Upload upload = new Upload(null, receiver);
   private File doc;

   private Label fileUploadInfoLabel;
   
   public static boolean isMimeTypeSupported(String mimeType) {
      switch (mimeType) {
      case "text/plain":
      case "text/xml":
      case "text/html":
      case "image/png":
      case "image/gif":
      case "image/jpg":
         return true;
      }
      return false;
   }

   public FileContentSubWindow(WindowType type, File doc) {

      setWidth("50%");
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setHeight("100%");
      setContent(subContent);

      switch (type) {
         case CONTENT:
            this.doc = doc;
            setCaption("View file " + doc.getName());
            setHeight("50%");
            createContentView(doc, subContent);
            break;

        case UPLOAD:
           this.doc = doc.getParentFile();
           setCaption("Upload to " + doc.getAbsolutePath());
           setHeight("20%");
           createUploadView(doc, subContent);
           upload.addSucceededListener(receiver);
           break;
      }


   }

   private void createUploadView(File doc, VerticalLayout subContent) {
      
      fileUploadInfoLabel = new Label();
      fileUploadInfoLabel.setEnabled(false);
      subContent.addComponent(fileUploadInfoLabel);
      
      upload.setImmediate(true);
      subContent.addComponent(upload);
   }

   private void createContentView(File doc, VerticalLayout subContent) {
      
      if (isImage(doc)) {
         Embedded image = new Embedded();
//         image.setHeight("100%");
//         image.setWidth("100%");
         image.setSource(new FileResource(doc));
         subContent.addComponent(image);
      } else {
         TextArea text = new TextArea(new TextFileProperty(doc));
         text.setHeight("100%");
         text.setWidth("100%");
         subContent.addComponent(text);
      }
   }

   private boolean isImage(File doc) {
      String mimeType = FileTypeResolver.getMIMEType(doc);
      switch (mimeType) {
         case MimeTypeUtils.IMAGE_JPEG_VALUE:
         case MimeTypeUtils.IMAGE_GIF_VALUE:
         case MimeTypeUtils.IMAGE_PNG_VALUE:
            return true;
      }
      return false;
   }

   enum WindowType {
      CONTENT,
      UPLOAD;
   }
   
   public class MyReceiver implements Receiver, SucceededListener {

      /** serialVersionUID */
      private static final long serialVersionUID = -5844542658116931976L;
      private final transient Logger LOGGER = LoggerFactory.getLogger(MyReceiver.class);

      public File file;

      public OutputStream receiveUpload(String filename, String mimeType) {
          // Create upload stream
          FileOutputStream fos = null; // Stream to write to
          try {
              // Open the file for writing.
              file = new File(doc.getAbsolutePath() + "/" + filename);
              fos = new FileOutputStream(file);
          } catch (final java.io.FileNotFoundException e) {
              new Notification("Could not open file<br/>", e.getMessage(), Notification.Type.ERROR_MESSAGE).show(Page.getCurrent());
              return null;
          }
          return fos; // Return the output stream to write to
      }

      @Override
      public void uploadSucceeded(SucceededEvent event) {
         fileUploadInfoLabel.setValue("The fileupload to " + file.getAbsolutePath() + " succeed.");
         fileUploadInfoLabel.setEnabled(true);
         upload.setEnabled(false);
      }
  }      
}
