package org.openthinclient.web.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.Binder;
import com.vaadin.data.BinderValidationStatus;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.*;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;


public class CreateDirectorySubWindow extends Window {

   /** serialVersionUID */
   private static final long serialVersionUID = 6056187481962333854L;
   
   private static final Logger LOGGER = LoggerFactory.getLogger(CreateDirectorySubWindow.class);
   private String ALLOWED_FILENAME_PATTERN = "[\\w]+";

   public CreateDirectorySubWindow(FileBrowserView fileBrowserView, Path doc, Path managerHome) {

      addCloseListener(event -> {
         UI.getCurrent().removeWindow(this);
      });
      
      IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      Path dir;
      if (doc == null) {
         dir = managerHome;
      } else if (Files.isDirectory(doc)) {
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

      String newPath = mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_PROMPT);
      Binder<String> newPathBinder = new Binder<>();
      newPathBinder.setBean(newPath);

      TextField tf = new TextField();
      tf.setWidth("260px");
      tf.setCursorPosition(0);
      group.addComponent(tf);
      newPathBinder.forField(tf)
                   .withValidator(new RegexpValidator(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_REGEX), ALLOWED_FILENAME_PATTERN, true))
                   .withValidator(new StringLengthValidator(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_EMPTY), 1, 99));

      group.addComponent(new Button(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_SAVE), event -> {        
         BinderValidationStatus<String> validationStatus = newPathBinder.validate();
         if (validationStatus.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            validationStatus.getBeanValidationErrors().forEach(validationResult -> {
               sb.append(validationResult.getErrorMessage()).append("\n");
            });
            errorMessage.setCaption(sb.toString());
            errorMessage.setVisible(true);
            return;
         }

         Path newDir = dir.resolve(tf.getValue());
         LOGGER.debug("Create new directory: ", newDir);
         try {
            Path path = Files.createDirectory(newDir);
            LOGGER.debug("Created new directory: ", path);
            fileBrowserView.refresh(path);
         } catch (Exception exception) {
            Notification.show(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_FAILED, newDir.getFileName()), Type.ERROR_MESSAGE);
         }
         this.close();
      }));
      
   }
   
 
}
