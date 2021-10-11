package org.openthinclient.wizard.check;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public abstract class AbstractCheck<T> implements Callable<CheckExecutionResult<T>> {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final String name;
  private final String description;

  public AbstractCheck(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public final CheckExecutionResult<T> call() throws Exception {

    log.info("Starting check...");
    CheckExecutionResult<T> result = perform();

    log.info("Check finished. Result: " + result);
    return result;

  }

  protected abstract CheckExecutionResult<T> perform();


}
