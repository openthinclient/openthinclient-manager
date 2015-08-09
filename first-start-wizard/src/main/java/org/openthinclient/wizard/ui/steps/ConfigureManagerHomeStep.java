package org.openthinclient.wizard.ui.steps;

import com.vaadin.data.Validator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.wizard.model.CheckStatus;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.teemu.wizards.Wizard;

import java.util.Collections;
import java.util.List;

public class ConfigureManagerHomeStep extends AbstractCheckExecutingStep {

  private final SystemSetupModel systemSetupModel;
  private final VerticalLayout content;
  private final TextField homeDirectoryTextField;
  private CheckEnvironmentStep.CheckStatusLabel checkStatusLabel;

  public ConfigureManagerHomeStep(Wizard wizard, SystemSetupModel systemSetupModel) {
    super(wizard);
    this.systemSetupModel = systemSetupModel;

    homeDirectoryTextField = new TextField("Home directory", systemSetupModel.getManagerHomeModel().getManagerHomePathProperty());
    homeDirectoryTextField.setWidth(100, Sizeable.Unit.PERCENTAGE);
    homeDirectoryTextField.setStyleName(ValoTheme.TEXTFIELD_LARGE);
    homeDirectoryTextField.addValidator(new StringLengthValidator("A valid directory name must be specified", 3, null, false));
    content = new VerticalLayout(

            createLabelH1(getCaption()),
            createLabelHuge("In this step, the manager home directory will be specified. The manager home is the base directory containing all data relevant to the openthinclient system."),

            homeDirectoryTextField

    );
    content.setSpacing(true);
    content.setMargin(true);
    setContent(content);

  }

  @Override
  public String getCaption() {
    return "Configure Home Directory";
  }

  @Override
  public boolean onAdvance() {

    try {
      homeDirectoryTextField.commit();
    } catch (Validator.InvalidValueException e) {
      return false;
    }

    if (systemSetupModel.getManagerHomeModel().isManagerHomeSpecified()) {
      runChecks();

      return false;
    }

    return systemSetupModel.getManagerHomeModel().isManagerHomeValid();
  }

  @Override
  public boolean onBack() {
    return true;
  }

  @Override
  protected List<CheckStatusLabel> getStatusLabels() {
    if (checkStatusLabel != null) {
      return Collections.singletonList(checkStatusLabel);
    }

    return Collections.emptyList();
  }

  @Override
  protected void onRunChecks() {

    // execute the manager home validation
    final CheckStatus checkStatus = systemSetupModel.getManagerHomeModel().runCheck();

    if (checkStatusLabel != null) {
      content.removeComponent(checkStatusLabel);
    }
    checkStatusLabel = new CheckEnvironmentStep.CheckStatusLabel(checkStatus);
    content.addComponent(checkStatusLabel);
  }

  @Override
  protected void onChecksFinished() {
    // nothing to do
  }

  @Override
  protected boolean isChecksFinished() {
    return systemSetupModel.getManagerHomeModel().isManagerHomeValidated();
  }
}
