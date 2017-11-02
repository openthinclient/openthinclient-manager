package org.openthinclient.pkgmgr.progress;

import org.openthinclient.progress.ListenableProgressFuture;
import org.openthinclient.progress.ProgressManager;
import org.openthinclient.progress.ProgressTask;
import org.openthinclient.progress.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class PackageManagerExecutionEngine {

    private static final Logger LOG = LoggerFactory.getLogger(PackageManagerExecutionEngine.class);

    private final ThreadPoolTaskExecutor executor;
    private final AtomicReference<ProgressManager<?>> currentTask;
    private final CopyOnWriteArrayList<ProgressManager<?>> queuedTasks;
    private final CopyOnWriteArrayList<TaskActivatedHandler> taskActivatedHandlers;
    private final CopyOnWriteArrayList<TaskFinalizedHandler> taskFinalizedHandlers;

    public PackageManagerExecutionEngine(ThreadPoolTaskExecutor executor) {
        this.executor = executor;
        currentTask = new AtomicReference<>();
        queuedTasks = new CopyOnWriteArrayList<>();
        taskActivatedHandlers = new CopyOnWriteArrayList<>();
        taskFinalizedHandlers = new CopyOnWriteArrayList<>();
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
        progressManager.onTaskFinalization(this::taskFinalized);

        queuedTasks.add(progressManager);

        final ListenableFuture<V> future = executor.submitListenable(progressManager.asCallable());

        return progressManager.wrap(future);

    }

    private <V> void taskFinalized(ProgressManager<V> progressManager) {
        queuedTasks.remove(progressManager);
        currentTask.set(null);
        taskFinalizedHandlers.forEach(handler -> {
            try {
                handler.taskFinalized(progressManager.getFuture());
            } catch (Exception e) {
                LOG.error("task finalized handler failed", e);
            }
        });
    }

    private <V> void taskActivated(ProgressManager<V> progressManager) {
        currentTask.set(progressManager);
        taskActivatedHandlers.forEach(handler -> {
            try {
                handler.taskActivated(progressManager.getFuture());
            } catch (Exception e) {
                LOG.error("task activation handler failed", e);
            }
        });
    }

    public Registration addTaskActivatedHandler(TaskActivatedHandler handler) {
        taskActivatedHandlers.add(handler);
        return () -> taskActivatedHandlers.remove(handler);
    }

    public Registration addTaskFinalizedHandler(TaskFinalizedHandler handler) {
        taskFinalizedHandlers.add(handler);
        return () -> taskFinalizedHandlers.remove(handler);
    }

    public interface TaskActivatedHandler {
        void taskActivated(ListenableProgressFuture<?> task);
    }

    public interface TaskFinalizedHandler {
        void taskFinalized(ListenableProgressFuture<?> task);
    }

}
