package org.openthinclient.progress;

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
    public ProgressReceiver subprogress(double progressMin, double progressMax) {
        return this;
    }

    @Override
    public void completed() {

    }
}
