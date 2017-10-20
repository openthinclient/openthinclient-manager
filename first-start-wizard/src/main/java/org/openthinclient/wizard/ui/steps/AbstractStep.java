package org.openthinclient.wizard.ui.steps;

import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public abstract class AbstractStep implements WizardStep {

  private Component content;

  protected IMessageConveyor mc;
  
  public AbstractStep() {
    mc = new MessageConveyor(UI.getCurrent().getLocale());
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
    return createLabelLarge(text, ContentMode.TEXT);
  }

  protected Label createLabelLarge(String text, ContentMode contentMode) {
    final Label label = new Label(text, contentMode);
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
