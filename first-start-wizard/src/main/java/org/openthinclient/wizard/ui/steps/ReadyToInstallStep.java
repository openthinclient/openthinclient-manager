package org.openthinclient.wizard.ui.steps;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;
import org.vaadin.teemu.wizards.Wizard;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ReadyToInstallStep extends AbstractStep {

  public ReadyToInstallStep(final Wizard wizard) {

    final VerticalLayout layout = new VerticalLayout();
    layout.setMargin(true);
    layout.setSpacing(true);

    layout.addComponent(createLabelHuge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_READYTOINSTALLSTEP_HEADLINE)));

    layout.addComponent(createLabelLarge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_READYTOINSTALLSTEP_HEAD_TEXT)));
    layout.addComponent(new Label(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_READYTOINSTALLSTEP_TEXT)));

    final Button button = new Button(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_READYTOINSTALLSTEP_BUTTON_INSTALL), FontAwesome.DOWNLOAD);
    button.addClickListener(e -> wizard.next());
    button.addStyleName(ValoTheme.BUTTON_HUGE);
    button.addStyleName(ValoTheme.BUTTON_PRIMARY);
    layout.addComponent(button);

    setContent(layout);

  }

  @Override
  public String getCaption() {
    return mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_READYTOINSTALLSTEP_TITLE);
  }

  @Override
  public boolean onAdvance() {
    return true;
  }

  @Override
  public boolean onBack() {
    return true;
  }
}
