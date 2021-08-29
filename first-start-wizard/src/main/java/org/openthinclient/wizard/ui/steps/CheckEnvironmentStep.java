package org.openthinclient.wizard.ui.steps;

import java.util.List;
import java.util.stream.Collectors;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;
import org.openthinclient.wizard.model.CheckStatus;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

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
    contents.addComponent(createLabelH1(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CHECKENVIRONMENTSTEP_HEADLINE)));
    contents.addComponent(createLabelLarge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CHECKENVIRONMENTSTEP_TEXT)));

    this.statusLabels = systemSetupModel.getCheckEnvironmentModel().getCheckStates()
            .stream()
            .map(this::createCheckstatusComponent)
            .collect(Collectors.toList());

    // add all check components to the main layout
    getStatusLabels().forEach(contents::addComponent);

    runChecksButton = new Button(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CHECKENVIRONMENTSTEP_BUTTON_RUN), e -> runChecks());
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
    runChecksButton.setCaption(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CHECKENVIRONMENTSTEP_BUTTON_RERUN));
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
    return mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CHECKENVIRONMENTSTEP_TITLE);
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
