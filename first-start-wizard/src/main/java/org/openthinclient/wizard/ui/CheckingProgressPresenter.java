package org.openthinclient.wizard.ui;

import org.openthinclient.advisor.check.AbstractCheck;
import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.check.CheckExecutionResult;
import org.openthinclient.advisor.check.CheckTask;

import java.util.function.Consumer;

public class CheckingProgressPresenter {

  private final CheckExecutionEngine checkExecutionEngine;
  private final View view;
  private final Consumer<Result> resultConsumer;
  private CheckTask<?> task;
  private volatile CheckExecutionResult.CheckResultType currentResultType;

  public CheckingProgressPresenter(CheckExecutionEngine checkExecutionEngine, View view, Consumer<Result> resultConsumer) {
    this.checkExecutionEngine = checkExecutionEngine;
    this.view = view;
    this.resultConsumer = resultConsumer;

    view.setOnOkHandler(this::handleOnOK);
    view.setOnCancelHandler(this::handleOnCancel);
  }

  private void handleOnCancel(View view) {
    if (task != null)
      task.cancel();
    resultConsumer.accept(Result.CANCEL);
    view.close();
  }

  private void handleOnOK(View view) {
    if (currentResultType != null) {
      switch (currentResultType) {

        case WARNING:
          // FIXME warning should be handled differently!
        case SUCCESS:
          resultConsumer.accept(Result.SUCCESS_OK);
          break;
        case FAILED:
          resultConsumer.accept(Result.ERROR);
          break;
      }
      view.close();
    }
  }

  public void execute(AbstractCheck<?> check) {
    if (task != null) {
      throw new IllegalStateException("this presenter has already been executed. Re-execution is not supported.");
    }
    view.setInProgress();

    this.task = checkExecutionEngine.execute(check);
    this.task.onResult((result) -> {
      currentResultType = result.getType();

      // we have to "access the ui" as we're receiving the result on a separate worker thread.
      view.accessUI(view -> {
        // FIXME i18n!!!
        switch (result.getType()) {
          case FAILED:
            view.setError("Connection check failed.");
            break;
          case WARNING:
            // FIXME warning should be handled differently.
          case SUCCESS:
            view.setSuccess("Internet connection working properly");
        }
      });
    });

  }

  public enum Result {
    SUCCESS_OK,
    CANCEL,
    ERROR
  }


  public interface View {

    void accessUI(Consumer<View> consumer);

    void setInProgress();

    void setSuccess(String message);

    void setError(String message);

    void setOnOkHandler(Consumer<View> handler);

    void setOnCancelHandler(Consumer<View> handler);

    void close();
  }

}
