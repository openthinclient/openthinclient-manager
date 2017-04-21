package org.openthinclient.web.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.ui.*;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.themes.ValoTheme;
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
      
      setCaption(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_REMOVE_CAPTION, doc.getFileName()));
      setHeight("120px");
      setWidth("500px");
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setSizeFull();
      setContent(subContent);
      
      CssLayout group = new CssLayout();
      group.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      subContent.addComponent(group);

      TextField tf = new TextField();
//      tf.setInputPrompt(doc.getFileName().toString());
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
            if (exception instanceof DirectoryNotEmptyException) {
               Notification.show(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_REMOVE_FOLDERNOTEMPTY, dir.getFileName()), Type.ERROR_MESSAGE);
            } else {
               Notification.show(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_REMOVE_FAIL, dir.getFileName()), Type.ERROR_MESSAGE);
            }
         }
         this.close();
      }));
   }
   
 
}
