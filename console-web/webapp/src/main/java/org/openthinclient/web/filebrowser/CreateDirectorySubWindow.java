package org.openthinclient.web.filebrowser;

import java.nio.file.Files;
import java.nio.file.Path;

import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;


public class CreateDirectorySubWindow extends Window {

   /** serialVersionUID */
   private static final long serialVersionUID = 6056187481962333854L;
   
   private static final Logger LOGGER = LoggerFactory.getLogger(CreateDirectorySubWindow.class);
   private String ALLOWED_FILENAME_PATTERN = "[\\w]+";
   
   public CreateDirectorySubWindow(FileBrowserView fileBrowserView, Path doc) {
      
      addCloseListener(event -> {
         UI.getCurrent().removeWindow(this);
      });
      
      IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      Path dir;
      if (Files.isDirectory(doc)) {
         dir = doc;
      } else {
         dir = doc.getParent();
      }
      setCaption(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_CAPTION, dir.getFileName()));
      setHeight("140px");
      setWidth("500px");
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setSizeFull();
      setContent(subContent);
      

      CssLayout group = new CssLayout();
      group.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      subContent.addComponent(group);
      
      Label errorMessage = new Label();
      errorMessage.setVisible(false);
      subContent.addComponent(errorMessage);

      TextField tf = new TextField();
      tf.setInputPrompt(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_PROMPT));
      tf.setWidth("260px");
      tf.setCursorPosition(0);
      tf.addValidator(new RegexpValidator(ALLOWED_FILENAME_PATTERN, true, mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_REGEX)));
      tf.addValidator(new StringLengthValidator(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_EMPTY), 1, 99, true));
      tf.setValidationVisible(false);
      group.addComponent(tf);

      group.addComponent(new Button(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_SAVE), event -> {        
          
         try {
             tf.setValidationVisible(true);
             tf.validate();
         } catch (InvalidValueException e) {
             errorMessage.setCaption(e.getMessage());
             errorMessage.setVisible(true);
             return;
         }
         
         Path newDir = dir.resolve(tf.getValue());
         LOGGER.debug("Create new directory: ", newDir);
         try {
            Path path = Files.createDirectory(newDir);
            fileBrowserView.refresh();
            LOGGER.debug("Created new directory: ", path);
         } catch (Exception exception) {
            Notification.show(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_FAILED, newDir.getFileName()), Type.ERROR_MESSAGE);
         }
         this.close();
      }));
      
   }
   
 
}
