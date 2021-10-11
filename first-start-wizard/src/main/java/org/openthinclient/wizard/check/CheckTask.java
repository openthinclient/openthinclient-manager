package org.openthinclient.wizard.check;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.function.Consumer;

public class CheckTask<T> {
  private static final Logger LOG = LoggerFactory.getLogger(CheckTask.class);

  private final ListenableFuture<CheckExecutionResult<T>> future;

  public CheckTask(ListenableFuture<CheckExecutionResult<T>> future) {
    this.future = future;
  }

  public void onResult(Consumer<CheckExecutionResult<T>> consumer) {
    future.addCallback(
            // normal behaviour: invoke the consumer
            consumer::accept,
            // in case of an error completely unhandled error, log and inform the listener
            (ex) -> {
              LOG.error("Check execution failed.", ex);
              consumer.accept(new CheckExecutionResult<>(CheckExecutionResult.CheckResultType.FAILED));
            });
  }

  public void cancel() {
    try {
      future.cancel(true);
    } catch (Exception e) {
      LOG.error("Failed to cancel running check task", e);
    }
  }

}
