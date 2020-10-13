package org.openthinclient.service.update;

import org.springframework.context.ApplicationEvent;

public class UpdateRunnerEvent extends ApplicationEvent {
  private Integer exitValue;

  UpdateRunnerEvent(Object source, Integer exitValue) {
    super(source);
    this.exitValue = exitValue;
  }

  public boolean failed() {
    return this.exitValue != 0;
  }

  public Integer getExitValue() {
    return this.exitValue;
  }
}
