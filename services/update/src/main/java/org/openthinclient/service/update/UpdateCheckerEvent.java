package org.openthinclient.service.update;

import org.springframework.context.ApplicationEvent;

public class UpdateCheckerEvent extends ApplicationEvent {
  private boolean failed;

  UpdateCheckerEvent(Object source, boolean failed) {
    super(source);
    this.failed = failed;
  }

  public boolean failed() {
    return this.failed;
  }
}
