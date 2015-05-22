package org.openthinclient.wizard.ui.steps;

import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;
import org.vaadin.teemu.wizards.WizardStep;

public class CheckEnvironmentStep implements WizardStep {
  @Override
  public String getCaption() {
    return "Verify Environment";
  }

  @Override
  public Component getContent() {
    return new VerticalLayout();
  }

  @Override
  public boolean onAdvance() {
    return false;
  }

  @Override
  public boolean onBack() {
    return false;
  }
}
