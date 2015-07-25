package org.openthinclient.wizard.ui.steps;

import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.advisor.check.AbstractCheck;
import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

import java.util.List;

public class CheckEnvironmentStep extends AbstractStep implements WizardStep {

  private final Wizard wizard;
  private final CheckExecutionEngine checkExecutionEngine;
  private final List<AbstractCheck<?>> checks;
  private volatile boolean checksRunning;

  public CheckEnvironmentStep(Wizard wizard, CheckExecutionEngine checkExecutionEngine, List<AbstractCheck<?>> checks) {
    this.wizard = wizard;
    this.checkExecutionEngine = checkExecutionEngine;
    this.checks = checks;
    final VerticalLayout contents = new VerticalLayout();
    contents.setMargin(true);
    contents.addComponent(createLabelH1("Verify Environment"));
    contents.addComponent(createLabelLarge("We're now ready to execute some environment health checks. These checks will investigate your current runtime environment whether or not it is suitable for the openthinclient manager."));

    checks.forEach(check -> addCheck(contents, check));

    final Button runChecksButton = new Button("Run checks", e -> {
      checksRunning = true;
    });
    runChecksButton.setStyleName(ValoTheme.BUTTON_LARGE);
    contents.addComponent(runChecksButton);

    setContent(contents);

  }

  private void addCheck(VerticalLayout contents, AbstractCheck<?> check) {
    contents.addComponent(new Label(check.getName()));
  }

  @Override
  public String getCaption() {
    return "Verify Environment";
  }

  @Override
  public boolean onAdvance() {
    return false;
  }

  @Override
  public boolean onBack() {
    return !checksRunning;
  }
}
