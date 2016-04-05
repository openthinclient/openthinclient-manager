package org.openthinclient.pkgmgr.op;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The {@link InstallPlan} represents a sequence of {@link InstallPlanStep steps} to be executed in
 * order. The install plan is the unit of work that the package manager will perform to install,
 * uninstall or update/downgrade packages.
 *
 * The whole {@link InstallPlan} is expressed in a sequence of {@link InstallPlanStep steps} that
 * shall be executed in the order that they are listed.
 */
public class InstallPlan {

  private final List<InstallPlanStep> steps = new ArrayList<>();

  public List<InstallPlanStep> getSteps() {
    return steps;
  }

  public Stream<InstallPlanStep.PackageInstallStep> getPackageInstallSteps() {
    return steps.stream().filter(step -> step instanceof InstallPlanStep.PackageInstallStep).map(step -> (InstallPlanStep.PackageInstallStep) step);
  }

  public Stream<InstallPlanStep.PackageVersionChangeStep> getPackageVersionChangeSteps() {
    return steps.stream().filter(step -> step instanceof InstallPlanStep.PackageVersionChangeStep).map(step -> (InstallPlanStep.PackageVersionChangeStep) step);
  }
}
