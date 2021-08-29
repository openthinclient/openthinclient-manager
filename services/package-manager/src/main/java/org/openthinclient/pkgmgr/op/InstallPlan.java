package org.openthinclient.pkgmgr.op;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.openthinclient.pkgmgr.db.Package;

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

  public Stream<InstallPlanStep.PackageUninstallStep> getPackageUninstallSteps() {
    return steps.stream().filter(step -> step instanceof InstallPlanStep.PackageUninstallStep).map(step -> (InstallPlanStep.PackageUninstallStep) step);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("steps", steps).toString();
  }

  /**
   * Creates an InstallPlan map, this maps:
   * <li>PackageInstallStep::getPackage</li>
   * <li>PackageUninstallStep::getInstalledPackage</li>
   * <li>PackageVersionChangeStep::getTargetPackage</li>
   * @return Map<InstallPlanStep, Package> contains InstallPlanStep and affecting packages
   */
  public Map<InstallPlanStep, Package> getInstallPlanStepMap() {
    Map<InstallPlanStep, Package> installPlanMap = new HashMap<>();
      installPlanMap.putAll(getPackageInstallSteps().collect(Collectors
          .toMap(Function.identity(), InstallPlanStep.PackageInstallStep::getPackage)));
      installPlanMap.putAll(getPackageUninstallSteps().collect(Collectors
          .toMap(Function.identity(), InstallPlanStep.PackageUninstallStep::getInstalledPackage)));
      installPlanMap.putAll(getPackageVersionChangeSteps().collect(Collectors
          .toMap(Function.identity(), InstallPlanStep.PackageVersionChangeStep::getTargetPackage)));
    return installPlanMap;
  }

}
