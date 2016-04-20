package org.openthinclient.web.filebrowser;

import java.nio.file.Path;

import org.springframework.util.MimeTypeUtils;

import com.vaadin.data.util.TextFileProperty;
import com.vaadin.server.FileResource;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.util.FileTypeResolver;


public class ContentViewSubWindow extends Window {

   /** serialVersionUID */
   private static final long serialVersionUID = -6794768759901017749L;

   public ContentViewSubWindow(FileBrowserView fileBrowserView, Path doc) {
      
      addCloseListener(event -> {
         UI.getCurrent().removeWindow(this);
      });
      
      setCaption("View file " + doc.getFileName());
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
         TextArea text = new TextArea(new TextFileProperty(doc.toFile()));
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
