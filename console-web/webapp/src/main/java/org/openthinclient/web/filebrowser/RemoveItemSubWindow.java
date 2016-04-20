package org.openthinclient.web.filebrowser;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;


public class RemoveItemSubWindow extends Window {
   
   /** serialVersionUID */
   private static final long serialVersionUID = 9051875905013039615L;
   private static final Logger LOGGER = LoggerFactory.getLogger(RemoveItemSubWindow.class);
   
   public RemoveItemSubWindow(FileBrowserView fileBrowserView, Path doc) {
      
      addCloseListener(event -> {
         UI.getCurrent().removeWindow(this);
      });
      
      setCaption("Remove folder " + doc.getFileName());
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
            if (exception instanceof DirectoryNotEmptyException) {
               Notification.show("Directory '" + dir.getFileName() + "' not empty.", Type.ERROR_MESSAGE);
            } else {
               Notification.show("Failed to remove directory '" + dir.getFileName() + "'.", Type.ERROR_MESSAGE);
            }
         }
         this.close();
      }));
   }
   
 
}
