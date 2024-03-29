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

   private static final long serialVersionUID = 6056187481962333854L;

   private static final Logger LOGGER = LoggerFactory.getLogger(CreateDirectorySubWindow.class);
   private static final String ALLOWED_FILENAME_PATTERN = "[\\.\\w-+_]([ \\.\\w-+_]*[\\w-+_])?";
   private static final String NOT_RESERVED_FILENAME_PATTERN = "(?i)(?!(aux|clock\\$|con|nul|prn|com[0-9]|lpt[0-9])(?:$|\\.)).*";

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

      Binder<String> newPathBinder = new Binder<>();
      newPathBinder.setBean(new String());

      TextField tf = new TextField();
      tf.setWidth("260px");
      tf.setCursorPosition(0);
      tf.setPlaceholder(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_PROMPT));
      group.addComponent(tf);
      newPathBinder.forField(tf)
                   .withValidator(new StringLengthValidator(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_LENGTH), 1, 255))
                   .withValidator(new RegexpValidator(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_CHARACTERS), ALLOWED_FILENAME_PATTERN))
                   .withValidator(new RegexpValidator(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_SPECIAL), NOT_RESERVED_FILENAME_PATTERN))
                   .bind(String::toString, (s, s2) -> new String(s));

      group.addComponent(new Button(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_SAVE), event -> {
         BinderValidationStatus<String> validationStatus = newPathBinder.validate();
         if (validationStatus.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            validationStatus.getFieldValidationStatuses().forEach(fvs -> {
               sb.append(fvs.getMessage().get()).append("\n");
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
