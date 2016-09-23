package org.openthinclient.wizard.ui.steps;

import org.vaadin.spring.i18n.I18N;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.VerticalLayout;

public class IntroStep extends AbstractStep implements WizardStep {
  
  public IntroStep(I18N i18n) {
    super(i18n);
  }

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


    layout.addComponent(createLabelHuge(i18n.get("ui.firststart.installsteps.introstep.title")));
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
