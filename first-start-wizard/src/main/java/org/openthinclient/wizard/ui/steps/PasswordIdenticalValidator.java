package org.openthinclient.wizard.ui.steps;

import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.ui.PasswordField;

public class PasswordIdenticalValidator extends AbstractValidator<String> {
  private final PasswordField otherPasswordField;

  public PasswordIdenticalValidator(PasswordField otherPasswordField) {
    super("Passwords do not match");
    this.otherPasswordField = otherPasswordField;
  }

  @Override
  protected boolean isValidValue(String value) {
    return otherPasswordField.getValue().equals(value);
  }

  @Override
  public Class<String> getType() {
    return String.class;
  }
}
