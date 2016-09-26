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
     */
    void progress(String message);

    /**
     * Updates the current progress value.
     *
     * @param progress a value beween 0.0 and 1.0 indicating 0% or 100% progress or {@link
     *                 ListenableProgressFuture#INDETERMINATE} for indeterminate progress
     */
    void progress(double progress);

    /**
     * Creates a sub {@link ProgressReceiver} that will translate all progress information into a
     * range of <code>progressMin-progressMax</code>.
     */
    ProgressReceiver subprogress(double progressMin, double progressMax);

    void completed();
}
