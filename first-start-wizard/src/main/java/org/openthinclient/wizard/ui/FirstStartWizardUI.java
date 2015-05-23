package org.openthinclient.wizard.ui;

import com.vaadin.annotations.Theme;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.wizard.ui.steps.CheckEnvironmentStep;
import org.openthinclient.wizard.ui.steps.ConfigureNetworkStep;
import org.openthinclient.wizard.ui.steps.IntroStep;
import org.vaadin.spring.annotation.VaadinUI;
import org.vaadin.spring.annotation.VaadinUIScope;
import org.vaadin.teemu.wizards.Wizard;

@Theme("otc-wizard")
@VaadinUI
@VaadinUIScope
public class FirstStartWizardUI extends UI {

  private Wizard wizard;

  @Override
  protected void init(VaadinRequest request) {
    wizard = createWizard();

    final VerticalLayout wizardWrapper = new VerticalLayout();
    wizardWrapper.setMargin(true);
    wizardWrapper.setSpacing(true);
    wizardWrapper.setSizeFull();
    wizardWrapper.addComponent(wizard);

    // create the root layout and add the wizard
    final VerticalLayout root = new VerticalLayout();
    root.setSizeFull();

    root.addComponent(createHeader());
    root.addComponent(wizardWrapper);
    root.setExpandRatio(wizardWrapper, 1.0f);


    setContent(root);


  }

  private Wizard createWizard() {
    Wizard wizard = new Wizard();
    wizard.setSizeFull();
    wizard.setUriFragmentEnabled(true);

    wizard.addStep(new IntroStep(), "welcome");
    wizard.addStep(new ConfigureNetworkStep(), "config-network");
    wizard.addStep(new CheckEnvironmentStep(), "environment-check");
    return wizard;
  }

  private CssLayout createHeader() {
    final CssLayout header = new CssLayout();

    final Image otcLogo = new Image();
    otcLogo.setSource(new ThemeResource("img/otc_toplogo32.png"));
    header.addComponent(otcLogo);
    return header;
  }
}
