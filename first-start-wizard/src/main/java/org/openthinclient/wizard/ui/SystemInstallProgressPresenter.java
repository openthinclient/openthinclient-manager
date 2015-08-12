package org.openthinclient.wizard.ui;

import com.vaadin.ui.UI;
import org.openthinclient.wizard.install.AbstractInstallStep;
import org.openthinclient.wizard.model.InstallModel;

import java.util.ArrayList;
import java.util.List;

public class SystemInstallProgressPresenter {

  private final InstallModel installModel;
  private final List<StepAndView> stepAndViews;

  public SystemInstallProgressPresenter(InstallModel installModel) {
    this.installModel = installModel;
    stepAndViews = new ArrayList<>();
  }

  public void present(UI ui, View view) {

    ui.setPollInterval(500);
    ui.addPollListener(event -> update());


    view.setTitle("System Installation");
    view.setDescription("Your openthinclient system is being installed.");

    installModel.getInstallSystemTask().getSteps().forEach(step -> {
      final InstallItemView itemView = view.addItemView();
      itemView.setTitle(step.getName());
      stepAndViews.add(new StepAndView(step, itemView));
    });

    update();
  }

  private void update() {
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


  }

  public interface View {
    void setTitle(String title);

    void setDescription(String description);

    InstallItemView addItemView();
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
