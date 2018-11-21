package org.openthinclient.wizard.ui.steps;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;
import org.openthinclient.manager.util.installation.InstallationDirectoryUtil;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.VerticalLayout;

public class IntroStep extends AbstractStep implements WizardStep {

  private SystemSetupModel systemSetupModel;

  public IntroStep(SystemSetupModel systemSetupModel) {
    this.systemSetupModel = systemSetupModel;
  }

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

    if (!InstallationDirectoryUtil.isInstallationDirectoryEmpty(systemSetupModel.getFactory().getManagerHomeDirectory())) {
      Label note = createLabelLarge(
          mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_INTROSTEP_CLEAN_MANAGERHOME_NOTE,
              systemSetupModel.getFactory().getManagerHomeDirectory()), ContentMode.HTML);
      note.addStyleName("color-red");
      layout.addComponent(note);
    }

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
