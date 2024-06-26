package org.openthinclient.web.support;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import org.openthinclient.service.update.UpdateChecker;
import org.openthinclient.service.update.UpdateCheckerEvent;
import org.openthinclient.service.update.UpdateRunner;
import org.openthinclient.service.update.UpdateRunnerEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.SettingsUI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.util.Optional;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SpringView(name = UpdateManagerView.NAME, ui = SettingsUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SUPPORT_APPLICATION_HEADER", order = 70)
public class UpdateManagerView extends Panel implements View {

  public final static String NAME = "support";

  private static final long serialVersionUID = -8836300902351197949L;

  @Autowired
  private UpdateChecker updateChecker;
  @Autowired
  private UpdateRunner updateRunner;

  @Value("${application.version}")
  private String applicationVersion;

  private EventBus eventBus;

  private MessageConveyor mc;
  private CssLayout root;

  private Button updateCheckerButton;
  private Label updateCheckerButtonLabel;
  private Label newVersionLabel;
  private CssLayout updateRunnerContainer;
  private Button updateRunnerButton;
  private Label updateRunnerButtonLabel;
  private Label descriptionLabel;

  private UI ui;

  public UpdateManagerView(EventBus.SessionEventBus eventBus) {
     this.eventBus = eventBus;

     mc = new MessageConveyor(UI.getCurrent().getLocale());
     setSizeFull();

     root = new CssLayout();
     root.setStyleName("updateview");
     setContent(root);
  }

  @Override
  public void attach() {
      super.attach();
      eventBus.subscribe(this);
  }

  @Override
  public void detach() {
      eventBus.unsubscribe(this);
      super.detach();
  }

  @Override
  public String getCaption() {
     return mc.getMessage(UI_SUPPORT_APPLICATION_HEADER);
  }

  @PostConstruct
  private void init() {
    ui = UI.getCurrent();

    Label versionInformation = new Label(mc.getMessage(UI_SUPPORT_CURRENT_APPLICATION_VERSION, applicationVersion));
    versionInformation.addStyleName("versionInformation");

    this.newVersionLabel = new Label();
    updateNewVersionLabel();

    root.addComponents(
      versionInformation,
      newVersionLabel,
      buildUpdateChecker(),
      buildUpdateRunner(),
      buildWikilinks()
    );

    if(updateChecker.isRunning()) {
      displayUpdateCheckerRunning();
    }
  }

  private void updateNewVersionLabel() {
    if(updateChecker.getNewVersion().isPresent()) {
      this.newVersionLabel.setCaption(mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_UPDATE, updateChecker.getNewVersion().get()));
      this.newVersionLabel.setIcon(VaadinIcons.EXCLAMATION_CIRCLE_O);
    } else {
      this.newVersionLabel.setCaption(mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_OK));
      this.newVersionLabel.setIcon(VaadinIcons.CHECK);
    }
  }

  private CssLayout buildUpdateChecker() {
    CssLayout updateCheckerContainer = new CssLayout();
    updateCheckerContainer.addStyleName("update-checker");
    this.updateCheckerButton = new Button(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_CHECK_APPLICATION_VERSION_BUTTON));
    this.updateCheckerButtonLabel = new Label();
    updateCheckerContainer.addComponents(this.updateCheckerButton, this.updateCheckerButtonLabel);

    this.updateCheckerButton.addClickListener(e -> {
        displayUpdateCheckerRunning();
        updateChecker.fetchNewVersion();
    });

    return updateCheckerContainer;
  }

  private void displayUpdateCheckerRunning() {
    this.updateCheckerButtonLabel.setValue(mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_RUNNING));
    this.updateCheckerButton.setEnabled(false);
    this.updateRunnerButton.setEnabled(false);
    this.descriptionLabel.setEnabled(false);
  }

  private CssLayout buildUpdateRunner() {
    this.updateRunnerContainer = new CssLayout();
    updateRunnerContainer.addStyleName("update-runner");
    this.updateRunnerButton = new Button(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_BUTTON));
    this.updateRunnerButtonLabel = new Label(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_INFO), ContentMode.HTML);
    this.descriptionLabel = new Label(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_DESCRIPTION));
    this.descriptionLabel.addStyleName("description");
    updateRunnerContainer.addComponents(updateRunnerButton, updateRunnerButtonLabel, this.descriptionLabel);

    this.updateRunnerContainer.setVisible(updateChecker.getNewVersion().isPresent());

    if(updateRunner.isRunning()) {
      displayUpdateRunnerRunning();
    }

    updateRunnerButton.addClickListener(e -> {
      displayUpdateRunnerRunning();
      updateRunner.run();
    });

    return updateRunnerContainer;
  }

  private void displayUpdateRunnerRunning() {
    this.updateRunnerButtonLabel.setValue(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_RUNNING));
    this.updateCheckerButton.setEnabled(false);
    this.updateRunnerButton.setEnabled(false);
    this.descriptionLabel.setEnabled(false);
  }

  private CssLayout buildWikilinks() {
    CssLayout wikilinks = new CssLayout();
    wikilinks.addComponents(
      new Label(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_WIKI_CAPTION)),
      new Label(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_WIKI_VERSION_INFORMATION), ContentMode.HTML)
      );
    wikilinks.addStyleName("wikilinks");
    return wikilinks;
  }

  @EventBusListenerMethod
  private void updateCheckerFinished(UpdateCheckerEvent event) {
    this.updateCheckerButton.setEnabled(true);
    this.updateRunnerButton.setEnabled(true);
    this.descriptionLabel.setEnabled(true);
    if(event.failed()) {
      this.updateCheckerButtonLabel.setValue(mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_FAIL));
    } else {
      Optional<String> newVersion = updateChecker.getNewVersion();
      if(newVersion.isPresent()) {
        this.updateCheckerButtonLabel.setValue(mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_UPDATE, newVersion.get()));
      } else {
        this.updateCheckerButtonLabel.setValue(mc.getMessage(UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_NOUPDATE));
      }
      updateNewVersionLabel();
      this.updateRunnerContainer.setVisible(newVersion.isPresent());
    }
    ui.push();
  }

  @EventBusListenerMethod
  private void updateRunnerFinished(UpdateRunnerEvent event) {
    if(event.failed()) {
      this.updateCheckerButton.setEnabled(true);
      this.updateRunnerButton.setEnabled(true);
      this.descriptionLabel.setEnabled(true);
      if(event.getExitValue() == 107) {
        this.updateRunnerButtonLabel.setValue(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_NONE));
      } else {
        this.updateRunnerButtonLabel.setValue(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_ERROR));
      }
    } else {
      this.updateRunnerButtonLabel.setValue(mc.getMessage(UI_SUPPORT_APPLICATION_UPDATE_INSTALLING));
      Page page = ui.getPage();
      page.setLocation("/VAADIN/splash.html#" + ui.getUiRootPath() + "#" + page.getUriFragment());
    }
    ui.push();
  }

}
