package org.openthinclient.wizard.ui.steps;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.ValueContext;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.ui.PasswordField;

public class PasswordIdenticalValidator extends AbstractValidator<String> {
  private final PasswordField otherPasswordField;

  public PasswordIdenticalValidator(PasswordField otherPasswordField) {
    super("Passwords do not match");
    this.otherPasswordField = otherPasswordField;
  }

  @Override
  public ValidationResult apply(String value, ValueContext context) {
    return otherPasswordField.getValue().equals(value) ? ValidationResult.ok() : ValidationResult.error("Passwords do not match");
  }
}
