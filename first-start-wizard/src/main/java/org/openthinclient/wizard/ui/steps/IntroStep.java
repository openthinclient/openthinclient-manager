package org.openthinclient.wizard.ui.steps;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.VerticalLayout;

public class IntroStep extends AbstractStep implements WizardStep {
  
  @Override
  public String getCaption() {
    return mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_INTROSTEP_TITLE);
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

    layout.addComponent(createLabelHuge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_INTROSTEP_TITLE)));
    layout.addComponent(createLabelLarge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_INTROSTEP_TEXT)));

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
