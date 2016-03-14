package org.openthinclient.pkgmgr.progress;

import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class ProgressManager<V> {

    private final ProgressTask<V> task;
    private Consumer<ProgressManager<V>> taskActivationHandler;
    private Consumer<ProgressManager<V>> taskFinalizationHandler;
    private ListenableProgressFuture<V> future;
    private volatile State state;

    public ProgressManager(ProgressTask<V> task) {
        this.task = task;
        state = State.QUEUED;
    }

    public State getState() {
        return state;
    }

    public ProgressReceiver receiver() {

        return new ProgressReceiver() {
            @Override
            public void progress(String message, double progress) {
                // FIXME
            }

            @Override
            public void progress(String message) {
                // FIXME
            }

            @Override
            public void progress(double progress) {
                // FIXME
            }

            @Override
            public void completed() {
// FIXME
            }
        };

    }

    public void onTaskActivation(Consumer<ProgressManager<V>> taskActivationHandler) {
        this.taskActivationHandler = taskActivationHandler;
    }

    public void onTaskFinalization(Consumer<ProgressManager<V>> taskFinalizationHandler) {
        this.taskFinalizationHandler = taskFinalizationHandler;
    }

    public ListenableProgressFuture<V> wrap(final ListenableFuture<V> future) {

        if (this.future != null) {
            throw new IllegalStateException("this progress manager instance already wrapped a future");
        }

        this.future = new ListenableProgressFuture<V>() {
            @Override
            public double getProgress() {
                // FIXME there should be some kind of real progress!
                return INDETERMINATE;
            }

            @Override
            public String getProgressMessage() {
                return null;
            }

            @Override
            public void addCallback(ListenableFutureCallback<? super V> callback) {
                future.addCallback(callback);
            }

            @Override
            public void addCallback(SuccessCallback<? super V> successCallback,
                                    FailureCallback failureCallback) {
                future.addCallback(successCallback, failureCallback);
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return future.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return future.isCancelled();
            }

            @Override
            public boolean isDone() {
                return future.isDone();
            }

            @Override
            public V get() throws InterruptedException, ExecutionException {
                return future.get();
            }

            @Override
            public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return future.get(timeout, unit);
            }
        };

        return this.future;
    }

    public Callable<V> asCallable() {

        return () -> {
            try {
                state = State.RUNNING;
                if (taskActivationHandler != null)
                    taskActivationHandler.accept(this);
                final V result = task.execute(receiver());
                state = State.FINISHED;
                return result;
            } catch (Exception e) {
                state = State.FAILED;
                throw e;
            } finally {

                if (taskFinalizationHandler != null)
                    taskFinalizationHandler.accept(this);
            }
        };

    }

    public ListenableProgressFuture<?> getFuture() {
        return future;
    }

    public enum State {
        QUEUED,
        RUNNING,
        FINISHED,
        FAILED
    }
}
