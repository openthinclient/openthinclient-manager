package org.openthinclient.pkgmgr.progress;

public interface ProgressReceiver {

    /**
     * Sends a progress notification including a message.
     *
     * @param progress a value beween 0.0 and 1.0 indicating 0% or 100% progress or {@link
     *                 ListenableProgressFuture#INDETERMINATE} for indeterminate progress
     */
    void progress(String message, double progress);

    /**
     * Sends or updates a progress message.
     *
     * @param message
     */
    void progress(String message);

    /**
     * Updates the current progress value.
     *
     * @param progress a value beween 0.0 and 1.0 indicating 0% or 100% progress or {@link
     *                 ListenableProgressFuture#INDETERMINATE} for indeterminate progress
     */
    void progress(double progress);

    void completed();
}
