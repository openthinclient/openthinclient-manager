package org.openthinclient.progress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingProgressReceiver implements ProgressReceiver {

    private static Logger LOGGER = LoggerFactory.getLogger(LoggingProgressReceiver.class);

    @Override
    public void progress(String message, double progress) {
        LOGGER.info(message + ", " + progress + "\r");
    }

    @Override
    public void progress(String message) {
        LOGGER.info(message + "\r");
    }

    @Override
    public void progress(double progress) {
        LOGGER.info(progress + "\r");
    }

    @Override
    public ProgressReceiver subprogress(double progressMin, double progressMax) {
        return this;
    }

    @Override
    public void completed() {
        LOGGER.info("completed");
    }
}
