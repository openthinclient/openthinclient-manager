package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.*;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.FileResource;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.*;
import com.vaadin.util.FileTypeResolver;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.service.UserService;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_USERS_PASSWORD_RETYPE_VALIDATOR;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_USERS_PASSWORD_VALIDATOR_LENGTH;


public class UserProfileSubWindow extends Window {

   /** serialVersionUID */
   private static final long serialVersionUID = -6L;
  IMessageConveyor mc;

  public UserProfileSubWindow(UserService service, User directoryObject) {
      
      mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addCloseListener(event -> {
         UI.getCurrent().removeWindow(this);
      });
      
      setCaption(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_VIEWFILE_CAPTION, directoryObject.getName()));
      setHeight("400px");
      setWidth("500px");
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setSizeFull();
      setContent(subContent);

      subContent.addComponent(buildForm(service, directoryObject));

   }

   private Component buildForm(UserService service, User directoryObject) {

     FormLayout layoutWithBinder = new FormLayout();
     Binder<DirectoryObject> binder = new Binder<>();

     Label infoLabel = new Label();

// Create the fields
     TextField name = new TextField();
     name.setValueChangeMode(ValueChangeMode.EAGER);
     TextField description = new TextField();
     description.setValueChangeMode(ValueChangeMode.EAGER);
     PasswordField passwordField = new PasswordField();
     passwordField.setValueChangeMode(ValueChangeMode.EAGER);
     PasswordField passwordRetype = new PasswordField();
     passwordRetype.setValueChangeMode(ValueChangeMode.EAGER);

     NativeButton save = new NativeButton("Save");
     NativeButton reset = new NativeButton("Reset");

     layoutWithBinder.addComponent(name);
     layoutWithBinder.addComponent(description);
     layoutWithBinder.addComponent(passwordField);
     layoutWithBinder.addComponent(passwordRetype);

// Button bar
     HorizontalLayout actions = new HorizontalLayout();
     actions.addComponents(save, reset);
     // save.setStyleName("marginRight");

// First name and last name are required fields
     name.setRequiredIndicatorVisible(true);
     description.setRequiredIndicatorVisible(true);

     binder.setBean(directoryObject);
     binder.forField(name)
         .bind(DirectoryObject::getName, DirectoryObject::setName);
     binder.forField(description)
         .bind(DirectoryObject::getDescription, DirectoryObject::setDescription);

     String pwdValue = directoryObject.getUserPassword() != null ? new String(directoryObject.getUserPassword()) : null;
     binder.forField(passwordField)
         .withValidator(new StringLengthValidator(mc.getMessage(UI_USERS_PASSWORD_VALIDATOR_LENGTH), 5, 15))
         .bind(directoryObject1 -> new String(((User)directoryObject1).getUserPassword()),
              (directoryObject1, s) -> ((User)directoryObject1).setNewPassword(s));

     binder.forField(passwordRetype)
         .withValidator(new AbstractValidator(mc.getMessage(UI_USERS_PASSWORD_RETYPE_VALIDATOR)) {
           @Override
           public ValidationResult apply(Object value, ValueContext context) {
             return toResult(value, passwordField.getValue() != null && passwordField.getValue().equals(value));
           }
           @Override
           public Object apply(Object o, Object o2) {
             return null;
           }
         })
         .bind(o -> null, (o, o2) -> {});

// Click listeners for the buttons
     save.addClickListener(event -> {
       if (binder.writeBeanIfValid(directoryObject)) {
//         LOGGER.info("Save: " + profile);
         service.save(directoryObject);
         infoLabel.setValue("Saved bean values: " + directoryObject);
       } else {
         BinderValidationStatus<DirectoryObject> validate = binder.validate();
         String errorText = validate.getFieldValidationStatuses()
             .stream().filter(BindingValidationStatus::isError)
             .map(BindingValidationStatus::getMessage)
             .map(Optional::get).distinct()
             .collect(Collectors.joining(", "));
         infoLabel.setValue("There are errors: " + errorText);
       }
     });
     reset.addClickListener(event -> {
       // clear fields by setting null
       binder.readBean(null);
       infoLabel.setValue("");
     });

     VerticalLayout vl = new VerticalLayout();
     vl.addComponents(layoutWithBinder, actions);
     return vl;
   }
   
}
