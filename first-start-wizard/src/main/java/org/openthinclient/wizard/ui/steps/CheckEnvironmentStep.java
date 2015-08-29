package org.openthinclient.wizard.ui.steps;

import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.wizard.model.CheckStatus;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

import java.util.List;
import java.util.stream.Collectors;

public class CheckEnvironmentStep extends AbstractCheckExecutingStep implements WizardStep {

  private final SystemSetupModel systemSetupModel;
  private final Button runChecksButton;
  private final List<CheckStatusLabel> statusLabels;

  public CheckEnvironmentStep(Wizard wizard, SystemSetupModel systemSetupModel) {
    super(wizard);
    this.systemSetupModel = systemSetupModel;
    final VerticalLayout contents = new VerticalLayout();
    contents.setMargin(true);
    contents.setSpacing(true);
    contents.addComponent(createLabelH1("Verify Environment"));
    contents.addComponent(createLabelLarge("We're now ready to execute some environment health checks. These checks will investigate your current runtime environment whether or not it is suitable for the openthinclient manager."));

    this.statusLabels = systemSetupModel.getCheckEnvironmentModel().getCheckStates()
            .stream()
            .map(this::createCheckstatusComponent)
            .collect(Collectors.toList());

    // add all check components to the main layout
    getStatusLabels().forEach(contents::addComponent);

    runChecksButton = new Button("Run checks", e -> runChecks());
    runChecksButton.setStyleName(ValoTheme.BUTTON_LARGE);
    contents.addComponent(runChecksButton);

    setContent(contents);

    updateStatusLabels();
  }

  @Override
  protected List<CheckStatusLabel> getStatusLabels() {
    return statusLabels;
  }

  @Override
  protected void onRunChecks() {
    systemSetupModel.getCheckEnvironmentModel().runChecks();
    runChecksButton.setEnabled(false);
  }

  @Override
  protected void onChecksFinished() {
    runChecksButton.setCaption("Rerun Checks");
    runChecksButton.setEnabled(true);
  }

  @Override
  protected boolean isChecksFinished() {
    return systemSetupModel.getCheckEnvironmentModel().allChecksRunned();
  }

  private CheckStatusLabel createCheckstatusComponent(CheckStatus checkStatus) {
    return new CheckStatusLabel(checkStatus);
  }

  @Override
  public String getCaption() {
    return "Environment";
  }

  @Override
  public boolean onAdvance() {
    return systemSetupModel.getCheckEnvironmentModel().isAcceptable();
  }

  @Override
  public boolean onBack() {
    return !systemSetupModel.getCheckEnvironmentModel().isRunning();
  }


}
