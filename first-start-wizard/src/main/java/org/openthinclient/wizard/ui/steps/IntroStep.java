package org.openthinclient.wizard.ui.steps;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.vaadin.teemu.wizards.WizardStep;

public class IntroStep extends AbstractStep implements WizardStep {
  @Override
  public String getCaption() {
    return "Welcome";
  }

  @Override
  public Component getContent() {
    final VerticalLayout layout = new VerticalLayout();

    layout.setMargin(true);
    layout.setSpacing(true);
//    layout.setSizeFull();


    final Image logoImage = new Image();
    logoImage.setSource(new ThemeResource("img/OpenThinClient-logo.svg.png"));
    layout.addComponent(logoImage);
    layout.setComponentAlignment(logoImage, Alignment.MIDDLE_CENTER);


    layout.addComponent(createLabelHuge("Welcome to the open thinclient manager."));
    layout.addComponent(createLabelLarge("This wizard will guide you through the first required steps to initialize your Environment."));


    return layout;
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
