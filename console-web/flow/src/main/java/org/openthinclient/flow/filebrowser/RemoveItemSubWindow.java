package org.openthinclient.flow.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.openthinclient.flow.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;


public class RemoveItemSubWindow extends Dialog {
   
   /** serialVersionUID */
   private static final long serialVersionUID = 9051875905013039615L;
   private static final Logger LOGGER = LoggerFactory.getLogger(RemoveItemSubWindow.class);
   
   public RemoveItemSubWindow(FileBrowserView fileBrowserView, Path doc) {
      
      addDialogCloseActionListener(event -> {
         UI.getCurrent().remove(this);
      });
      
      IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      add(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_REMOVE_CAPTION, ""));
      setHeight("140px");
      setWidth("500px");
//      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setSizeFull();
      add(subContent);
      
      Label tf = new Label(doc.getFileName().toString());
      subContent.add(tf);

      subContent.add(new Button(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_REMOVE_CAPTION), event -> {
         LOGGER.debug("Remove directory: ", doc);
         try {
            Path parent = doc.getParent();
            Files.delete(doc);
            fileBrowserView.refresh(parent);
         } catch (Exception exception) {
            if (exception instanceof DirectoryNotEmptyException) {
               Notification.show(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_REMOVE_FOLDERNOTEMPTY, doc.getFileName().toString()));
            } else {
               Notification.show(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_REMOVE_FAIL, doc.getFileName().toString()));
            }
         }
         this.close();
      }));
   }
   
 
}
