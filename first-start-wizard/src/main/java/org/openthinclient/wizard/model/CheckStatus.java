package org.openthinclient.wizard.model;

import org.openthinclient.wizard.check.AbstractCheck;
import org.openthinclient.wizard.check.CheckExecutionEngine;
import org.openthinclient.wizard.check.CheckExecutionResult;
import org.openthinclient.wizard.check.CheckTask;

/**
 * A small {@link AbstractCheck} wrapper, tracking the execution and the state of a given {@link AbstractCheck}.
 */
public class CheckStatus {
  private final AbstractCheck<?> check;
  private volatile CheckExecutionResult.CheckResultType resultType;
  private volatile boolean running;
  private volatile boolean finished;

  public CheckStatus(AbstractCheck<?> check) {
    this.check = check;
  }

  public CheckExecutionResult.CheckResultType getResultType() {
    return resultType;
  }

  public AbstractCheck<?> getCheck() {
    return check;
  }

  public boolean isRunning() {
    return running;
  }

  public boolean isFinished() {
    return finished;
  }

  public void executeOn(CheckExecutionEngine checkExecutionEngine) {
    running = true;
    finished = false;
    final CheckTask<?> task = checkExecutionEngine.execute(getCheck());
    task.onResult(executionResult -> {
      running = false;
      finished = true;
      resultType = executionResult.getType();
    });

  }
}
