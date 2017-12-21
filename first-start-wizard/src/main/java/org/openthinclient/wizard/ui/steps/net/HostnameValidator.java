package org.openthinclient.wizard.ui.steps.net;

import com.vaadin.data.validator.RegexpValidator;

public class HostnameValidator extends RegexpValidator {
  public HostnameValidator(String errorMessage) {
    super(errorMessage, "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");
  }


}
