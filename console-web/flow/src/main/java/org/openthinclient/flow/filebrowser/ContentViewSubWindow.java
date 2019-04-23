package org.openthinclient.flow.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import org.openthinclient.flow.i18n.ConsoleWebMessages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class ContentViewSubWindow extends Dialog {

   /** serialVersionUID */
   private static final long serialVersionUID = -6794768759901017749L;

   public ContentViewSubWindow(Path doc) {
      
      IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addDialogCloseActionListener(event -> {
         UI.getCurrent().remove(this);
      });
      
      add(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_VIEWFILE_CAPTION, doc.getFileName()));
      setHeight("400px");
      setWidth("500px");
//      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setSizeFull();
      add(subContent);
      
      if (isImage(doc)) {
//         Embedded image = new Embedded();
//         image.setSource(new FileResource(doc.toFile()));
//         subContent.addComponent(image);
      } else {
         TextArea text = new TextArea();
         try {
            text.setValue(new String(Files.readAllBytes(doc.toAbsolutePath())));
         } catch (IOException e) {
            // FIXME: how do we handle errors
            throw new RuntimeException("Cannot read file " + doc.toAbsolutePath());
         }
         text.setSizeFull();
         subContent.add(text);
      }
      
   }
   
   private boolean isImage(Path doc) {
//      String mimeType = FileTypeResolver.getMIMEType(doc.toFile());
//      switch (mimeType) {
//         case MimeTypeUtils.IMAGE_JPEG_VALUE:
//         case MimeTypeUtils.IMAGE_GIF_VALUE:
//         case MimeTypeUtils.IMAGE_PNG_VALUE:
//            return true;
//      }
      return false;
   }
   
}
