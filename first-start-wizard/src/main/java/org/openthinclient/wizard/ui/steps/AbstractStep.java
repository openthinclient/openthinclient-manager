package org.openthinclient.wizard.ui.steps;

import org.vaadin.spring.i18n.I18N;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

public abstract class AbstractStep implements WizardStep {

  private Component content;

  protected I18N i18n;
  
  public AbstractStep(I18N i18n) {
    this.i18n = i18n;
  }
  
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
