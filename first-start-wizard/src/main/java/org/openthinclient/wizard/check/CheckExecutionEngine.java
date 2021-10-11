package org.openthinclient.wizard.check;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

public class CheckExecutionEngine {

  private static final Logger LOG = LoggerFactory.getLogger(CheckExecutionEngine.class);

  private final AsyncListenableTaskExecutor taskExecutor;

  public CheckExecutionEngine() {
    this(createDefaultExecutor());
  }

  public CheckExecutionEngine(AsyncListenableTaskExecutor taskExecutor) {
    this.taskExecutor = taskExecutor;
  }

  /**
   * Creates a default {@link AsyncListenableTaskExecutor} which will run a single executor thread.
   *
   * @return
   */
  private static AsyncListenableTaskExecutor createDefaultExecutor() {
    final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setMaxPoolSize(1);
    return executor;
  }

  public <T> CheckTask<T> execute(AbstractCheck<T> check) {
    LOG.info("Executing check '" + check + "'");
    final ListenableFuture<CheckExecutionResult<T>> future = taskExecutor.submitListenable(check);

    return new CheckTask<>(future);

  }
}
