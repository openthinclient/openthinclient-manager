package org.openthinclient.web.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.server.FileResource;
import com.vaadin.ui.*;
import com.vaadin.util.FileTypeResolver;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class ContentViewSubWindow extends Window {

   /** serialVersionUID */
   private static final long serialVersionUID = -6794768759901017749L;

   public ContentViewSubWindow(FileBrowserView fileBrowserView, Path doc) {
      
      IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addCloseListener(event -> {
         UI.getCurrent().removeWindow(this);
      });
      
      setCaption(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_VIEWFILE_CAPTION, doc.getFileName()));
      setHeight("400px");
      setWidth("500px");
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setSizeFull();
      setContent(subContent);
      
      if (isImage(doc)) {
         Embedded image = new Embedded();
         image.setSource(new FileResource(doc.toFile()));
         subContent.addComponent(image);
      } else {
         TextArea text = new TextArea();
         try {
            text.setValue(new String(Files.readAllBytes(doc.toAbsolutePath())));
         } catch (IOException e) {
            // FIXME: how do we handle errors
            throw new RuntimeException("Cannot read file " + doc.toAbsolutePath());
         }
         text.setSizeFull();
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
   
}
