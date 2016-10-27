package org.openthinclient.runtime.control.cmd;

public abstract class AbstractCommand<T> {
  private final String name;

  public AbstractCommand(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public abstract T createOptionsObject();

  public abstract void execute(T options) throws Exception;
}
