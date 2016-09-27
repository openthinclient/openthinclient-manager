package org.openthinclient.wizard.ui;

import java.util.ArrayList;
import java.util.List;

import org.openthinclient.manager.runtime.util.RestartApplicationEvent;
import static org.openthinclient.wizard.FirstStartWizardMessages.*;
import org.openthinclient.wizard.install.AbstractInstallStep;
import org.openthinclient.wizard.install.InstallState;
import org.openthinclient.wizard.model.InstallModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import com.vaadin.ui.JavaScript;
import com.vaadin.ui.UI;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class SystemInstallProgressPresenter {

  private static final Logger LOG = LoggerFactory.getLogger(SystemInstallProgressPresenter.class);

  private final InstallModel installModel;
  private final List<StepAndView> stepAndViews;
  private final ApplicationEventPublisher publisher;

  protected IMessageConveyor mc;
  
  public SystemInstallProgressPresenter(ApplicationEventPublisher publisher, InstallModel installModel) {
    this.publisher = publisher;
    this.installModel = installModel;
    stepAndViews = new ArrayList<>();
    mc = new MessageConveyor(UI.getCurrent().getLocale());
  }

  public void present(UI ui, final View view) {

    ui.setPollInterval(500);
    ui.addPollListener(event -> update(view));

    view.setTitle(mc.getMessage(UI_FIRSTSTART_SYSTEMINSTALLPROGRESSPRESENTER_TITLE));
    view.setDescription(mc.getMessage(UI_FIRSTSTART_SYSTEMINSTALLPROGRESSPRESENTER_DESCRIPTION));

    installModel.getInstallSystemTask().getSteps().forEach(step -> {
      final InstallItemView itemView = view.addItemView();
      itemView.setTitle(step.getName());
      stepAndViews.add(new StepAndView(step, itemView));
    });

    update(view);
  }

  private void update(View view) {
    stepAndViews.forEach(stepAndView -> {
      switch (stepAndView.getStep().getState()) {
        case PENDING:
          stepAndView.getView().setPending();
          break;
        case RUNNING:
          stepAndView.getView().setRunning();
          break;
        case FINISHED:
          stepAndView.getView().setFinished();
          break;
        case FAILED:
          stepAndView.getView().setFailed();
          break;
      }
    });

    if (installModel.getInstallSystemTask().getInstallState() == InstallState.FINISHED) {
      view.setDescription(
          "Your System has been installed successfully. Click on the restart button below to restart the openthinclient manager.");
      view.enableRestartButton(() -> {
        LOG.info("\n\n==============================================\n" + " restarting\n"
            + "==============================================\n\n");

        // Restarting the whole application.
        // When running using the runtime standalone this will restart the whole application and
        // boot into the normal manager mode.
        publisher.publishEvent(new RestartApplicationEvent(this));

        // refresh page
        JavaScript.getCurrent().execute("window.setTimeout(window.location.reload.bind(window.location), 25000);");
      });
    }

  }



  public interface View {
    void setTitle(String title);

    void setDescription(String description);

    InstallItemView addItemView();

    void enableRestartButton(Runnable onButtonClicked);
  }

  public interface InstallItemView {

    void setTitle(String title);

    void setPending();

    void setRunning();

    void setFailed();

    void setFinished();


    void remove();
  }

  protected static final class StepAndView {
    private final AbstractInstallStep step;
    private final InstallItemView view;

    public StepAndView(AbstractInstallStep step, InstallItemView view) {

      this.step = step;
      this.view = view;
    }

    public AbstractInstallStep getStep() {
      return step;
    }

    public InstallItemView getView() {
      return view;
    }
  }

}
