package org.openthinclient.progress;

public abstract class AbstractProgressReceiver implements ProgressReceiver {

    @Override
    public ProgressReceiver subprogress(double progressMin, double progressMax) {
        return new SimpleSubprogressReceiver(this, progressMin, progressMax - progressMin);
    }

    public static class SimpleSubprogressReceiver implements ProgressReceiver {
        private final ProgressReceiver parent;
        private final double progressMin;
        private final double progressAmount;

        public SimpleSubprogressReceiver(ProgressReceiver parent, double progressMin, double progressAmount) {
            this.parent = parent;
            this.progressMin = progressMin;
            this.progressAmount = progressAmount;
        }

        @Override
        public void progress(String message, double progress) {
            parent.progress(message, progressMin + progress * progressAmount);
        }

        @Override
        public void progress(String message) {
            parent.progress(message);
        }

        @Override
        public void progress(double progress) {
            parent.progress(progressMin + progress * progressAmount);
        }

        @Override
        public ProgressReceiver subprogress(double progressMin, double progressMax) {
            return new SimpleSubprogressReceiver(this, progressMin, progressMax - progressMin);
        }

        @Override
        public void completed() {
            parent.completed();
        }
    }
}
