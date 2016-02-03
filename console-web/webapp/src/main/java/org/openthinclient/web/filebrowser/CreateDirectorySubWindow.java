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
   
   public CreateDirectorySubWindow(File doc, boolean create, FileBrowserView fileBrowserView) {

      super(create ? "Create" : "Remove" + " folder in " + doc.getPath());
      setHeight("17%");
      setWidth("30%");
      center();

      if (create) {
         createDirectory(doc, fileBrowserView);
      } else {
         removeDirectory(doc, fileBrowserView);
      }

   }
   
   private void removeDirectory(File doc, FileBrowserView fileBrowserView) {
      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setHeight("100%");

      CssLayout group = new CssLayout();
      group.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      subContent.addComponent(group);

      TextField tf = new TextField();
      tf.setInputPrompt(doc.getName());
      tf.setWidth("260px");
      tf.setEnabled(false);
      group.addComponent(tf);

      group.addComponent(new Button("Remove", event -> {        

         Path dir = new File(doc.getAbsolutePath() + "//" + tf.getValue()).toPath();
         LOGGER.debug("Remove directory: ", dir);
         
         try {
            Files.delete(dir);
            fileBrowserView.refresh();
         } catch (Exception exception) {
            Notification.show("Failed to remove directory '" + dir.getFileName() + "'.", Type.ERROR_MESSAGE);
         }
         this.close();
      }));

      setContent(subContent);
   }
   

   private void createDirectory(File doc, FileBrowserView fileBrowserView) {
      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setHeight("100%");

      CssLayout group = new CssLayout();
      group.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      subContent.addComponent(group);

      TextField tf = new TextField();
      tf.setInputPrompt("Foldername");
      tf.setWidth("260px");
      tf.setCursorPosition(0);
      group.addComponent(tf);

      group.addComponent(new Button("Save", event -> {        

         Path dir = new File(doc.getAbsolutePath() + "//" + tf.getValue()).toPath();
         LOGGER.debug("Create new directory: ", dir);
         
         try {
            Path path = Files.createDirectory(dir);
            fileBrowserView.refresh();
         } catch (Exception exception) {
            Notification.show("Failed to create directory '" + dir.getFileName() + "'.", Type.ERROR_MESSAGE);
         }
         this.close();
      }));

      setContent(subContent);
   }

}
