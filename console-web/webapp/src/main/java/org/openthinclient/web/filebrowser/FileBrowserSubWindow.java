package org.openthinclient.web.filebrowser;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.exolab.castor.xml.validators.StringValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeTypeUtils;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.TextFileProperty;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.FileResource;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.util.FileTypeResolver;

/**
 * The FileBrowser subWindow
 */
public class FileBrowserSubWindow extends Window  {

   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 3887697184057926390L;
   
   private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserSubWindow.class);

   private MyReceiver receiver = new MyReceiver();
   private Path doc; 

   private Label fileUploadInfoLabel;
   private FileBrowserView fileBrowserView;

   private String ALLOWED_FILENAME_PATTERN = "[\\w]+";
   
   public FileBrowserSubWindow(FileBrowserView fileBrowserView, WindowType type, Path doc) {

      this.fileBrowserView = fileBrowserView;
      addCloseListener(event -> {
         UI.getCurrent().removeWindow(this);
      });
      
      setWidth("50%");
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setHeight("100%");
      setContent(subContent);

      switch (type) {
         case CONTENT:
            this.doc = doc;
            setCaption("View file " + doc.getFileName());
            setHeight("50%");
            createContentView(subContent);
            break;

        case UPLOAD:
           if (Files.isDirectory(doc)) {
              this.doc = doc;
           } else {
              this.doc = doc.getParent();
           }
           setCaption("Upload to " + this.doc.getFileName());
           setHeight("20%");
           createUploadView(subContent);
           break;
           
        case CREATE_DIRECTORY:
           if (Files.isDirectory(doc)) {
              this.doc = doc;
           } else {
              this.doc = doc.getParent();
           }
           setCaption("Create folder in " + this.doc.getFileName());
           setHeight("140px");
           createDirectoryView(subContent);
           break;
           
        case REMOVE:
           this.doc = doc;
           setCaption("Remove folder " + doc.getFileName());
           setHeight("15%");
           removeDirectoryView(subContent);
           break;
      }
   }

   /**
    * Remove SubWindow
    * @param subContent VerticalLayout
    */
   private void removeDirectoryView(VerticalLayout subContent) {

      CssLayout group = new CssLayout();
      group.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      subContent.addComponent(group);

      TextField tf = new TextField();
      tf.setInputPrompt(doc.getFileName().toString());
      tf.setWidth("260px");
      tf.setEnabled(false);
      group.addComponent(tf);

      group.addComponent(new Button("Remove", event -> {        
         Path dir = doc.resolve(tf.getValue());
         LOGGER.debug("Remove directory: ", dir);
         try {
            Files.delete(dir);
            fileBrowserView.refresh();
         } catch (Exception exception) {
            Notification.show("Failed to remove directory '" + dir.getFileName() + "'.", Type.ERROR_MESSAGE);
         }
         this.close();
      }));

   }   
   
   /**
    * Create directory
    * @param subContent VerticalLayout
    */
   private void createDirectoryView(VerticalLayout subContent) {

      CssLayout group = new CssLayout();
      group.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      subContent.addComponent(group);
      
      Label errorMessage = new Label();
      errorMessage.setVisible(false);
      subContent.addComponent(errorMessage);

      TextField tf = new TextField();
      tf.setInputPrompt("Foldername");
      tf.setWidth("260px");
      tf.setCursorPosition(0);
      tf.addValidator(new RegexpValidator(ALLOWED_FILENAME_PATTERN, true, "The name has to be alphanumeric."));
      tf.addValidator(new StringLengthValidator("The name can not be empty.", 1, 99, true));
      tf.setValidationVisible(false);
      group.addComponent(tf);

      group.addComponent(new Button("Save", event -> {        
          
         try {
             tf.setValidationVisible(true);
             tf.validate();
         } catch (InvalidValueException e) {
             errorMessage.setCaption(e.getMessage());
             errorMessage.setVisible(true);
             return;
         }
         
         Path dir = doc.resolve(tf.getValue());
         LOGGER.debug("Create new directory: ", dir);
         try {
            Path path = Files.createDirectory(dir);
            fileBrowserView.refresh();
            LOGGER.debug("Created new directory: ", path);
         } catch (Exception exception) {
            Notification.show("Failed to create directory '" + dir.getFileName() + "'.", Type.ERROR_MESSAGE);
         }
         this.close();
      }));

   }   
   
   /**
    * Upload a file
    * @param subContent VerticalLayout
    */
   private void createUploadView(VerticalLayout subContent) {
      
      Upload upload = new Upload(null, receiver);
      upload.addSucceededListener(receiver);
      upload.setImmediate(true);
      subContent.addComponent(upload);
      
      fileUploadInfoLabel = new Label();
      fileUploadInfoLabel.setEnabled(false);
      subContent.addComponent(fileUploadInfoLabel);
   }
   
   
   /**
    * Show file content 
    * @param subContent VerticalLayout
    */
   private void createContentView(VerticalLayout subContent) {
      
      if (isImage(doc)) {
         Embedded image = new Embedded();
         image.setSource(new FileResource(doc.toFile()));
         subContent.addComponent(image);
      } else {
         TextArea text = new TextArea(new TextFileProperty(doc.toFile()));
         text.setHeight("100%");
         text.setWidth("100%");
         subContent.addComponent(text);
      }
   }

   private boolean isImage(Path doc) {
      String mimeType = FileTypeResolver.getMIMEType(doc.toFile());
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
      UPLOAD,
      CREATE_DIRECTORY,
      REMOVE;
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
              file = doc.resolve(filename);
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
