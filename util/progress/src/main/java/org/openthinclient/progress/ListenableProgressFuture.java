package org.openthinclient.progress;

import org.springframework.util.concurrent.ListenableFuture;

/**
 * A {@link java.util.concurrent.Future} (and {@link ListenableFuture}) extension that includes
 * org.openthinclient.progress reporting.
 */
public interface ListenableProgressFuture<T> extends ListenableFuture<T> {

    double INDETERMINATE = -1;

    /**
     * Returns the currently known org.openthinclient.progress of the {@link java.util.concurrent.Future} as a value in
     * the range from 0 (no org.openthinclient.progress) to 1 (done). If no org.openthinclient.progress could be determined a negative
     * value will be returned.
     */
    double getProgress();

    /**
     * Returns a message for the current org.openthinclient.progress. Might be <code>null</code> if no message is
     * available.
     */
    String getProgressMessage();

    Registration addProgressReceiver(ProgressReceiver receiver);

}
