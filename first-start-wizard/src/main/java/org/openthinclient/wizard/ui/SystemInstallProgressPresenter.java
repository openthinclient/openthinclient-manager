package org.openthinclient.wizard.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.ui.UI;
import org.openthinclient.manager.runtime.util.RestartApplicationEvent;
import org.openthinclient.wizard.install.AbstractInstallStep;
import org.openthinclient.wizard.install.InstallState;
import org.openthinclient.wizard.model.InstallModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_SYSTEMINSTALLPROGRESSPRESENTER_DESCRIPTION;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_SYSTEMINSTALLPROGRESSPRESENTER_TITLE;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_SYSTEMINSTALLPROGRESSPRESENTER_FINISHED_MESSAGE;

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
//          stepAndView.getView().setRunning();
          stepAndView.getView().setProgress(stepAndView.getStep().getProgress());
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
      view.setDescription(mc.getMessage(UI_FIRSTSTART_SYSTEMINSTALLPROGRESSPRESENTER_FINISHED_MESSAGE));
      view.enableRestartButton(() -> {
        LOG.info("\n\n==============================================\n" + " Starting\n"
            + "==============================================\n\n");

        // Restarting the whole application.
        // When running using the runtime standalone this will restart the whole application and
        // boot into the normal manager mode.
        publisher.publishEvent(new RestartApplicationEvent(this));

        // redirecting the user to the server restart page. This page will continously check whether the server successfully restarted.
        // Once started, the page will forward the user to the administration frontend.
        UI.getCurrent().getPage().setLocation("/restart/server-restart.html");
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

    void setProgress(double progress);
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
