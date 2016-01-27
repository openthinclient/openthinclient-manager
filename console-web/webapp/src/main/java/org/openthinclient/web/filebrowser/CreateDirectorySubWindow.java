package org.openthinclient.web.filebrowser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class CreateDirectorySubWindow extends Window {

   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 8344736579118414607L;

   private static final Logger LOGGER = LoggerFactory.getLogger(CreateDirectorySubWindow.class);
   
   public CreateDirectorySubWindow(File doc) {

      super("Create folder in " + doc.getPath());
      setHeight("10%");
      setWidth("30%");
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setHeight("100%");

      CssLayout group = new CssLayout();
      group.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      subContent.addComponent(group);

      TextField tf = new TextField();
      tf.setInputPrompt("Foldername");
      tf.setWidth("260px");
      group.addComponent(tf);

      group.addComponent(new Button("Save", event -> {        

         Path dir = new File(doc.getAbsolutePath() + "//" + tf.getValue()).toPath();
         LOGGER.debug("Create new directory: ", dir);
         
         try {
            Files.createDirectory(dir);
         } catch (Exception exception) {
            Notification.show("Failed to create directory " + dir.getFileName(), Type.ERROR_MESSAGE);
         }
         this.close();
      }));

      setContent(subContent);

   }

}
