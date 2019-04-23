package org.openthinclient.flow.filebrowser;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import org.openthinclient.flow.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;


public class CreateDirectorySubWindow extends Dialog {

   /** serialVersionUID */
   private static final long serialVersionUID = 6056187481962333854L;
   
   private static final Logger LOGGER = LoggerFactory.getLogger(CreateDirectorySubWindow.class);
   private String ALLOWED_FILENAME_PATTERN = "[\\w]+";

   public CreateDirectorySubWindow(FileBrowserView fileBrowserView, Path doc, Path managerHome) {

      addDialogCloseActionListener(event -> {
         UI.getCurrent().remove(this);
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
      add(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_CAPTION, dir.getFileName()));
      setHeight("140px");
      setWidth("500px");
//      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setSizeFull();
      add(subContent);
      

      Div group = new Div();
//      group.addStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
      subContent.add(group);
      
      Label errorMessage = new Label();
      errorMessage.setVisible(false);
      subContent.add(errorMessage);

      Binder<String> newPathBinder = new Binder<>();
      newPathBinder.setBean(new String());

      TextField tf = new TextField();
      tf.setWidth("260px");
//      tf.setCursorPosition(0);
      tf.setPlaceholder(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_PROMPT));
      group.add(tf);
      newPathBinder.forField(tf)
                   .withValidator(new RegexpValidator(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_REGEX), ALLOWED_FILENAME_PATTERN))
                   .withValidator(new StringLengthValidator(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_EMPTY), 1, 99))
                   .bind(String::toString, (s, s2) -> new String(s));

      group.add(new Button(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_SAVE), event -> {
         BinderValidationStatus<String> validationStatus = newPathBinder.validate();
         if (validationStatus.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            validationStatus.getFieldValidationStatuses().forEach(fvs -> {
               sb.append(fvs.getMessage().get()).append("\n");
            });
            errorMessage.setTitle(sb.toString());
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
            Notification.show(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_FAILED, newDir.getFileName()));
         }
         this.close();
      }));
      
   }
}
