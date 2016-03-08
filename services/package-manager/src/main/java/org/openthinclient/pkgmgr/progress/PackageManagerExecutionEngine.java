package org.openthinclient.pkgmgr.progress;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class PackageManagerExecutionEngine {

  private final ThreadPoolTaskExecutor executor;
  private final AtomicReference<ProgressManager<?>> currentTask;
  private final CopyOnWriteArrayList<ProgressManager<?>> queuedTasks;

  public PackageManagerExecutionEngine(ThreadPoolTaskExecutor executor) {
    this.executor = executor;
    currentTask = new AtomicReference<>();
    queuedTasks = new CopyOnWriteArrayList<>();
  }

  public ListenableProgressFuture<?> getCurrentTask() {
    final ProgressManager<?> cur = currentTask.get();
    if (cur != null) {
      return cur.getFuture();
    }
    return null;
  }

  public List<ProgressManager<?>> getQueuedTasks() {
    return queuedTasks;
  }

  public <V> ListenableProgressFuture<V> enqueue(final ProgressTask<V> task) {

    final ProgressManager<V> progressManager = new ProgressManager<>(task);
    progressManager.onTaskActivation(this::taskActivated);
    progressManager.onTaskFinalization(this::taskFinialized);

    queuedTasks.add(progressManager);

    final ListenableFuture<V> future = executor.submitListenable(progressManager.asCallable());

    return progressManager.wrap(future);

  }

  private <V> void taskFinialized(ProgressManager<V> progressManager) {
    queuedTasks.remove(progressManager);
    currentTask.set(null);
  }

  private <V> void taskActivated(ProgressManager<V> progressManager) {
    currentTask.set(progressManager);
  }
}
