package org.openthinclient.wizard.ui.steps;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.vaadin.teemu.wizards.Wizard;

public class ReadyToInstallStep extends AbstractStep {

  public ReadyToInstallStep(final Wizard wizard) {

    final VerticalLayout layout = new VerticalLayout();
    layout.setMargin(true);
    layout.setSpacing(true);

    layout.addComponent(createLabelHuge("Ready to install"));

    layout.addComponent(createLabelLarge("The required initial configuration has been done. You are now ready to install your openthinclient system."));
    layout.addComponent(new Label("Click on the 'Install System...' button below to execute the system installation."));

    final Button button = new Button("Install System...", FontAwesome.DOWNLOAD);
    button.addClickListener(e -> wizard.next());
    button.addStyleName(ValoTheme.BUTTON_HUGE);
    button.addStyleName(ValoTheme.BUTTON_PRIMARY);
    layout.addComponent(button);

    setContent(layout);

  }

  @Override
  public String getCaption() {
    return "Install";
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
