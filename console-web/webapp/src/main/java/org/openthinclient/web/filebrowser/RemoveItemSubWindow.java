package org.openthinclient.web.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.ui.*;
import com.vaadin.ui.Notification.Type;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;


public class RemoveItemSubWindow extends Window {
   
   /** serialVersionUID */
   private static final long serialVersionUID = 9051875905013039615L;
   private static final Logger LOGGER = LoggerFactory.getLogger(RemoveItemSubWindow.class);
   
   public RemoveItemSubWindow(FileBrowserView fileBrowserView, Path doc) {
      
      addCloseListener(event -> {
         UI.getCurrent().removeWindow(this);
      });
      
      IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      setCaption(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_REMOVE_CAPTION, ""));
      setHeight("140px");
      setWidth("500px");
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setSizeFull();
      setContent(subContent);
      
      Label tf = new Label(doc.getFileName().toString());
      subContent.addComponent(tf);

      subContent.addComponent(new Button(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_REMOVE_CAPTION), event -> {
         LOGGER.debug("Remove directory: ", doc);
         try {
            Path parent = doc.getParent();
            Files.delete(doc);
            fileBrowserView.refresh(parent);
         } catch (Exception exception) {
            if (exception instanceof DirectoryNotEmptyException) {
               Notification.show(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_REMOVE_FOLDERNOTEMPTY, doc.getFileName().toString()), Type.ERROR_MESSAGE);
            } else {
               Notification.show(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_REMOVE_FAIL, doc.getFileName().toString()), Type.ERROR_MESSAGE);
            }
         }
         this.close();
      }));
   }
   
 
}
