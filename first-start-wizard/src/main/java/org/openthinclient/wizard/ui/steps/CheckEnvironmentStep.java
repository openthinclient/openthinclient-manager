package org.openthinclient.wizard.ui.steps;

import com.vaadin.event.UIEvents;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.wizard.model.CheckEnvironmentModel;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

import java.util.List;
import java.util.stream.Collectors;

public class CheckEnvironmentStep extends AbstractStep implements WizardStep {

  private final Wizard wizard;
  private final SystemSetupModel systemSetupModel;
  private final Button runChecksButton;
  private final List<CheckStatusLabel> statusLabels;
  private final UIEvents.PollListener pollListener = this::onPoll;

  public CheckEnvironmentStep(Wizard wizard, SystemSetupModel systemSetupModel) {
    this.wizard = wizard;
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
    statusLabels.forEach(contents::addComponent);

    runChecksButton = new Button("Run checks", this::runChecks);
    runChecksButton.setStyleName(ValoTheme.BUTTON_LARGE);
    contents.addComponent(runChecksButton);

    setContent(contents);

    updateStatusLabels();
  }

  private void updateStatusLabels() {
    statusLabels.forEach(CheckStatusLabel::update);
  }

  private void runChecks(Button.ClickEvent event) {
    systemSetupModel.getCheckEnvironmentModel().runChecks();
    runChecksButton.setEnabled(false);
    wizard.getUI().setPollInterval(100);
    wizard.getUI().addPollListener(pollListener);
  }

  private void onPoll(UIEvents.PollEvent pollEvent) {

    updateStatusLabels();

    // have all checks been run?
    if (systemSetupModel.getCheckEnvironmentModel().allChecksRunned()) {
      // remove the listener and reset the poll mode
      wizard.getUI().removePollListener(pollListener);
      wizard.getUI().setPollInterval(-1);

      runChecksButton.setCaption("Rerun Checks");
      runChecksButton.setEnabled(true);
    }
  }

  private CheckStatusLabel createCheckstatusComponent(CheckEnvironmentModel.CheckStatus checkStatus) {
    return new CheckStatusLabel(checkStatus);
  }

  @Override
  public String getCaption() {
    return "Verify Environment";
  }

  @Override
  public boolean onAdvance() {
    return systemSetupModel.getCheckEnvironmentModel().isAcceptable();
  }

  @Override
  public boolean onBack() {
    return !systemSetupModel.getCheckEnvironmentModel().isRunning();
  }


  protected static final class CheckStatusLabel extends Label {
    private final CheckEnvironmentModel.CheckStatus checkStatus;

    public CheckStatusLabel(CheckEnvironmentModel.CheckStatus checkStatus) {
      this.checkStatus = checkStatus;
      setValue(checkStatus.getCheck().getName());
    }

    public void update() {
      if (checkStatus.getResultType() != null)
        switch (checkStatus.getResultType()) {
          case SUCCESS:
            setStyleName(ValoTheme.LABEL_SUCCESS);
            break;
          case WARNING:
            // FIXME
            break;
          case FAILED:
            setStyleName(ValoTheme.LABEL_FAILURE);
            break;
        }
    }
  }
}
