package org.openthinclient.pkgmgr.progress;

public interface ProgressTask<V> {

  V execute(ProgressReceiver progressReceiver);

}
