package org.openthinclient.pkgmgr.progress;

import org.springframework.util.concurrent.ListenableFuture;

/**
 * A {@link java.util.concurrent.Future} (and {@link ListenableFuture}) extension that includes progress reporting.
 *
 * @param <T>
 */
public interface ListenableProgressFuture<T> extends ListenableFuture<T> {

  double INDETERMINATE = -1;

  /**
   * Returns the currently known progress of the {@link java.util.concurrent.Future} as a value in the range from 0 (no
   * progress) to 1 (done). If no progress could be determined a negative value will be returned.
   *
   * @return
   */
  double getProgress();

  /**
   * Returns a message for the current progress. Might be <code>null</code> if no message is available.
   *
   * @return
   */
  String getProgressMessage();

}
