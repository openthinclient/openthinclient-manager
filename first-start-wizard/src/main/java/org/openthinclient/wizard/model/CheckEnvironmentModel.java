package org.openthinclient.wizard.model;

import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.check.CheckExecutionResult;
import org.openthinclient.advisor.check.CheckNetworkInferfaces;
import org.openthinclient.advisor.inventory.SystemInventory;

import java.util.ArrayList;
import java.util.List;

public class CheckEnvironmentModel {

  private final CheckExecutionEngine checkExecutionEngine;
  private final List<CheckStatus> checkStates;

  public CheckEnvironmentModel(SystemInventory systemInventory, CheckExecutionEngine checkExecutionEngine) {
    this.checkExecutionEngine = checkExecutionEngine;
    checkStates = new ArrayList<>();
    checkStates.add(new CheckStatus(new CheckNetworkInferfaces(systemInventory)));
  }

  public List<CheckStatus> getCheckStates() {
    return checkStates;
  }

  public void runChecks() {

    checkStates.forEach(check -> check.executeOn(checkExecutionEngine));

  }

  public boolean isRunning() {
    return checkStates.stream().anyMatch(CheckStatus::isRunning);
  }

  public boolean isAcceptable() {
    return checkStates.stream().allMatch(checkStatus -> !checkStatus.isRunning() && checkStatus.getResultType() != null && checkStatus.getResultType() != CheckExecutionResult.CheckResultType.FAILED);
  }

  public boolean allChecksRunned() {
    return checkStates.stream().allMatch(CheckStatus::isFinished);
  }

}
