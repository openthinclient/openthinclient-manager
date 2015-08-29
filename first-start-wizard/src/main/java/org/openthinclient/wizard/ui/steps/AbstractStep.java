package org.openthinclient.wizard.ui.steps;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;
import org.vaadin.teemu.wizards.WizardStep;

public abstract class AbstractStep implements WizardStep {

  private Component content;

  protected Label createLabelHuge(String text) {
    final Label label = new Label(text);
    label.setStyleName(ValoTheme.LABEL_HUGE);
    return label;
  }

  protected Label createLabelH1(String text) {
    final Label label = new Label(text);
    label.setStyleName(ValoTheme.LABEL_H1);
    return label;
  }

  protected Label createLabelLarge(String text) {
    final Label label = new Label(text);
    label.setStyleName(ValoTheme.LABEL_LARGE);
    return label;
  }

  @Override
  public Component getContent() {
    return content;
  }

  public void setContent(Component content) {
    this.content = content;
  }
}
