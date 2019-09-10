package org.openthinclient.service.update;

import org.springframework.context.ApplicationEvent;

public class UpdateRunnerEvent extends ApplicationEvent {
  private boolean failed;

  UpdateRunnerEvent(Object source, boolean failed) {
    super(source);
    this.failed = failed;
  }

  public boolean failed() {
    return this.failed;
  }
}
