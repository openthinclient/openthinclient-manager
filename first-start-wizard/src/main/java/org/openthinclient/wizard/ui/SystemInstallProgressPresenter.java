package org.openthinclient.wizard.ui;

import com.vaadin.ui.UI;
import org.openthinclient.wizard.install.AbstractInstallStep;
import org.openthinclient.wizard.install.InstallState;
import org.openthinclient.wizard.model.InstallModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tanukisoftware.wrapper.WrapperManager;

import java.util.ArrayList;
import java.util.List;

public class SystemInstallProgressPresenter {

  private static final Logger LOG = LoggerFactory.getLogger(SystemInstallProgressPresenter.class);

  private final InstallModel installModel;
  private final List<StepAndView> stepAndViews;

  public SystemInstallProgressPresenter(InstallModel installModel) {
    this.installModel = installModel;
    stepAndViews = new ArrayList<>();
  }

  public void present(UI ui, final View view) {

    ui.setPollInterval(500);
    ui.addPollListener(event -> update(view));


    view.setTitle("System Installation");
    view.setDescription("Your openthinclient system is being installed.");

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
      view.enableRestartButton(() -> {
        LOG.info("\n\n==============================================\n" +
                " restarting\n" +
                "==============================================\n\n");

        // Restarting the whole application.
        // When running using the runtime standalone this will restart the whole application and boot into the normal manager mode.
        WrapperManager.restart();
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
