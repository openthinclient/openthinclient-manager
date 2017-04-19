package org.openthinclient.wizard.model;

import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.check.CheckExecutionResult;
import org.openthinclient.advisor.check.CheckFilesystemFreeSpace;
import org.openthinclient.advisor.check.CheckNetworkInferfaces;
import org.openthinclient.advisor.inventory.SystemInventory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckEnvironmentModel {

  private final CheckExecutionEngine checkExecutionEngine;
  private final List<CheckStatus> checkStates;
  
  /** ManagerHomeModel to get current directory value */
  private ManagerHomeModel managerHomeModel;

  public CheckEnvironmentModel(SystemInventory systemInventory, CheckExecutionEngine checkExecutionEngine, ManagerHomeModel managerHomeModel, int installationFreespaceMinimum) {
    this.checkExecutionEngine = checkExecutionEngine;
    this.managerHomeModel = managerHomeModel;
    
    // FIXME: We need the current locale here, but UI.getCurrent() doesn't work 
    //        because CheckEnvironmentModel is instantiated via Sprinf-Bean, without UI
    Locale locale = Locale.GERMAN;
    
    checkStates = new ArrayList<>();
    checkStates.add(new CheckStatus(new CheckNetworkInferfaces(locale, systemInventory)));
    checkStates.add(new CheckStatus(new CheckFilesystemFreeSpace(locale, this::getManagerHome, installationFreespaceMinimum)));
  }

  public List<CheckStatus> getCheckStates() {
    return checkStates;
  }

  protected Path getManagerHome() {
    return Paths.get(managerHomeModel.getManagerHomePath().getAbsolutePath());
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

//  @Override
//  public String getStringParam(String key) {
//    
//    if (key != null && key.equals(CheckFilesystemFreeSpace.MANAGER_HOME_DIRECTORY)) {
//      return managerHomeModel.getManagerHomePathProperty().getValue();
//    }
//    
//    return null;
//  }

}
