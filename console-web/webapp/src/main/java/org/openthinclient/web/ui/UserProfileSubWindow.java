package org.openthinclient.web.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.*;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.*;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.service.UserService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.i18n.ConsoleWebMessages;

import java.util.Optional;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;


public class UserProfileSubWindow extends Window {

  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = -6L;
  IMessageConveyor mc;
  UserService service;

  boolean validatePassword = false;

  public UserProfileSubWindow(UserService service) {

    this.mc = new MessageConveyor(UI.getCurrent().getLocale());
    this.service = service;

    addCloseListener(event -> {
      UI.getCurrent().removeWindow(this);
    });
  }


  public void refresh(User user) {

    String userName =user.getName();

    setCaption(mc.getMessage(ConsoleWebMessages.UI_SETTINGS_ADMIN_WINDOWTITLE, userName));
    setHeight("400px");
    setWidth("500px");
    center();

    VerticalLayout subContent = new VerticalLayout();
    subContent.setSizeFull();
    setContent(subContent);

     FormLayout layoutWithBinder = new FormLayout();
     Binder<User> binder = new Binder<>();

     Label infoLabel = new Label();

     // Create the fields
     TextField name = new TextField(mc.getMessage(ConsoleWebMessages.UI_COMMON_NAME_LABEL));
     name.setValueChangeMode(ValueChangeMode.EAGER);
     TextField description = new TextField(mc.getMessage(ConsoleWebMessages.UI_COMMON_DESCRIPTION_LABEL));
     description.setValueChangeMode(ValueChangeMode.EAGER);
     PasswordField passwordField = new PasswordField(mc.getMessage(ConsoleWebMessages.UI_COMMON_PASSWORD_LABEL));
     passwordField.setValueChangeMode(ValueChangeMode.EAGER);
     PasswordField passwordRetype = new PasswordField(mc.getMessage(ConsoleWebMessages.UI_COMMON_PASSWORD_RETYPE_LABEL));
     passwordRetype.setValueChangeMode(ValueChangeMode.EAGER);

     NativeButton save = new NativeButton(mc.getMessage(ConsoleWebMessages.UI_BUTTON_SAVE));
     NativeButton reset = new NativeButton(mc.getMessage(ConsoleWebMessages.UI_BUTTON_RESET));

     layoutWithBinder.addComponent(name);
     layoutWithBinder.addComponent(description);
     layoutWithBinder.addComponent(passwordField);
     layoutWithBinder.addComponent(passwordRetype);

    // Button bar
     HorizontalLayout actions = new HorizontalLayout();
     actions.addComponents(save, reset);

    // First name is required field
     name.setRequiredIndicatorVisible(true);
     description.setRequiredIndicatorVisible(true);

     binder.setBean(user);
     binder.forField(name)
         .withValidator(new StringLengthValidator(mc.getMessage(UI_USERS_USERNAME_VALIDATOR_LENGTH), 5, 15))
         .bind(DirectoryObject::getName, DirectoryObject::setName);

     binder.forField(description)
         .bind(DirectoryObject::getDescription, DirectoryObject::setDescription);

     binder.forField(passwordField)
         .withValidator(new StringLengthValidator(mc.getMessage(UI_USERS_PASSWORD_VALIDATOR_LENGTH), 5, null))
         .bind(directoryObject1 -> new String((directoryObject1).getUserPassword()),
              (u, s) ->  { if (isValidatePassword()) u.setNewPassword(s);})
         .getField().addValueChangeListener(e -> {
           setPasswordChanged(true);
           infoLabel.setValue(mc.getMessage(UI_USERS_CHANGE_PASSWORD_HINT));
     });

     binder.forField(passwordRetype)
         .withNullRepresentation("") //
         .withValidator(new AbstractValidator(mc.getMessage(UI_USERS_PASSWORD_RETYPE_VALIDATOR)) {
           @Override
           public ValidationResult apply(Object value, ValueContext context) {
             if (isValidatePassword()) {
               return toResult(value, passwordField.getValue() != null && passwordField.getValue().equals(value));
             } else {
               return ValidationResult.ok();
             }
           }
           @Override
           public Object apply(Object o, Object o2) {
             return null;
           }
         })
         .bind(u -> new String(((User) u).getUserPassword()), (u, s) -> { if (isValidatePassword()) ((User) u).setVerifyPassword((String) s);})
         .getField().addValueChangeListener(e -> setPasswordChanged(true));


     // Click listener for save
     save.addClickListener(event -> {
       if (binder.writeBeanIfValid(user)) {
         user.getRealm().setNeedsRefresh();
         this.service.save(user);
         user.getRealm().fakePropertyChange();
         super.close();
       } else {
         BinderValidationStatus<User> validate = binder.validate();
         String errorText = validate.getFieldValidationStatuses()
             .stream().filter(BindingValidationStatus::isError)
             .map(BindingValidationStatus::getMessage)
             .map(Optional::get).distinct()
             .collect(Collectors.joining(", "));
         infoLabel.setValue("There are errors: " + errorText);
       }
     });
     reset.addClickListener(event -> {
       binder.readBean(service.findByName(userName));
       infoLabel.setValue("");
     });

     VerticalLayout vl = new VerticalLayout();
     vl.setSpacing(false);
     vl.setMargin(false);
     vl.addComponents(layoutWithBinder, infoLabel, actions);

    subContent.addComponent(vl);
   }

  public boolean isValidatePassword() {
    return validatePassword;
  }

  public void setPasswordChanged(boolean validatePassword) {
    this.validatePassword = validatePassword;
  }

  public void showError(Exception e) {

    String message;
    if (e.getCause() instanceof DirectoryException) {
      message = mc.getMessage(UI_ERROR_DIRECTORY_EXCEPTION);
    } else {
      message = e.getLocalizedMessage();
    }

    Label emptyScreenHint = new Label(
        VaadinIcons.WARNING.getHtml() + "&nbsp;&nbsp;&nbsp;" + mc.getMessage(UI_UNEXPECTED_ERROR) + message,
        ContentMode.HTML);
    emptyScreenHint.setStyleName("errorScreenHint");
    setContent(emptyScreenHint);
  }

}
