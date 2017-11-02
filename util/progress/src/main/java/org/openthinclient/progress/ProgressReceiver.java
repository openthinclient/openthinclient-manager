package org.openthinclient.progress;

public interface ProgressReceiver {

    /**
     * Sends a org.openthinclient.progress notification including a message.
     *
     * @param progress a value beween 0.0 and 1.0 indicating 0% or 100% org.openthinclient.progress or {@link
     *                 ListenableProgressFuture#INDETERMINATE} for indeterminate org.openthinclient.progress
     */
    void progress(String message, double progress);

    /**
     * Sends or updates a org.openthinclient.progress message.
     */
    void progress(String message);

    /**
     * Updates the current org.openthinclient.progress value.
     *
     * @param progress a value beween 0.0 and 1.0 indicating 0% or 100% org.openthinclient.progress or {@link
     *                 ListenableProgressFuture#INDETERMINATE} for indeterminate org.openthinclient.progress
     */
    void progress(double progress);

    /**
     * Creates a sub {@link ProgressReceiver} that will translate all org.openthinclient.progress information into a
     * range of <code>progressMin-progressMax</code>.
     */
    ProgressReceiver subprogress(double progressMin, double progressMax);

    void completed();
}
