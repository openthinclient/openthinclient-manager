package org.openthinclient.wizard.model;

import org.openthinclient.advisor.check.AbstractCheck;
import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.check.CheckExecutionResult;
import org.openthinclient.advisor.check.CheckNetworkInferfaces;
import org.openthinclient.advisor.check.CheckTask;
import org.openthinclient.advisor.inventory.SystemInventory;

import java.util.ArrayList;
import java.util.List;

public class CheckEnvironmentModel {

  private final SystemInventory systemInventory;
  private final CheckExecutionEngine checkExecutionEngine;
  private final List<CheckStatus> checkStates;

  public CheckEnvironmentModel(SystemInventory systemInventory, CheckExecutionEngine checkExecutionEngine) {
    this.systemInventory = systemInventory;
    this.checkExecutionEngine = checkExecutionEngine;
    checkStates = new ArrayList<>();
    checkStates.add(new CheckStatus(new CheckNetworkInferfaces(systemInventory)));
  }

  public List<CheckStatus> getCheckStates() {
    return checkStates;
  }

  public void runChecks() {

    checkStates.forEach(check -> {
      check.running = true;
      check.finished = false;
      final CheckTask<?> task = checkExecutionEngine.execute(check.getCheck());
      task.onResult(executionResult -> {
        check.running = false;
        check.finished = true;
        check.resultType = executionResult.getType();
      });
    });

  }

  public boolean isRunning() {
    return checkStates.stream().anyMatch(CheckStatus::isRunning);
  }

  public boolean isAcceptable() {
    return checkStates.stream().allMatch(checkStatus -> !checkStatus.isRunning() && checkStatus.getResultType() != null && checkStatus.getResultType() != CheckExecutionResult.CheckResultType.FAILED);
  }

  public boolean allChecksRunned() {
    return checkStates.stream().allMatch(checkStatus -> checkStatus.isFinished());
  }

  public static class CheckStatus {
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
  }
}
