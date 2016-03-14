package org.openthinclient.pkgmgr.progress;

public class NoopProgressReceiver implements ProgressReceiver {
    @Override
    public void progress(String message, double progress) {

    }

    @Override
    public void progress(String message) {

    }

    @Override
    public void progress(double progress) {

    }

    @Override
    public void completed() {

    }
}
