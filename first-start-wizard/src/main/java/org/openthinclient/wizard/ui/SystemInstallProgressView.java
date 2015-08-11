package org.openthinclient.wizard.ui;

import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.wizard.install.AbstractInstallStep;
import org.openthinclient.wizard.model.InstallModel;

import java.util.List;
import java.util.stream.Collectors;

public class SystemInstallProgressView extends VerticalLayout {

  private final List<InstallStatusLabel> statusLabels;
  private final InstallModel installModel;

  public SystemInstallProgressView(InstallModel installModel) {
    this.installModel = installModel;

    setSpacing(true);

    addComponent(createH1Label("System Installation"));
    addComponent(createLargeLabel("Your openthinclient system is being installed."));

    this.statusLabels = installModel.getInstallSystemTask().getSteps().stream()
            .map(InstallStatusLabel::new)
            .collect(Collectors.toList());
    this.statusLabels.forEach(this::addComponent);

    UI.getCurrent().setPollInterval(500);
    UI.getCurrent().addPollListener(event -> updateLabels());

    updateLabels();
  }

  private void updateLabels() {
    statusLabels.forEach(InstallStatusLabel::update);
  }

  private Label createH1Label(String label) {
    final Label l = new Label(label);
    l.setStyleName(ValoTheme.LABEL_H1);
    return l;
  }

  private Label createLargeLabel(String label) {
    final Label l = new Label(label);
    l.setStyleName(ValoTheme.LABEL_LARGE);
    return l;
  }

  protected static final class InstallStatusLabel extends Panel {
    private final AbstractInstallStep installStep;

    public InstallStatusLabel(AbstractInstallStep installStep) {
      this.installStep = installStep;

      final VerticalLayout content = new VerticalLayout();

      final Label label = new Label(installStep.getName());
      label.setStyleName(ValoTheme.LABEL_H2);

      content.addComponent(label);
      setContent(content);

    }

    public void update() {
      if (installStep.getState() != null)
        switch (installStep.getState()) {
          case FINISHED:
            setStyleName(ValoTheme.LABEL_SUCCESS);
            break;
          case FAILED:
            setStyleName(ValoTheme.LABEL_FAILURE);
            break;
          case PENDING:
            break;
          case RUNNING:
            setStyleName(ValoTheme.LABEL_SPINNER);
            break;

        }
    }
  }
}
