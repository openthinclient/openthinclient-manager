package org.openthinclient.progress;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DownloadProgressTrackingInputStream extends FilterInputStream {

    private final long contentLength;
    private final ProgressReceiver receiver;
    private long alreadyRead = 0;

    public DownloadProgressTrackingInputStream(InputStream in, long contentLength, ProgressReceiver receiver) {
        super(in);
        this.contentLength = contentLength;
        this.receiver = receiver;

    }

    @Override
    public int read() throws IOException {
        final int res = super.read();
        if (res >= 0) {
            updateProgress(1);
        }
        return res;
    }

    /**
     * compute the updated org.openthinclient.progress
     * @param count
     *
     */
    private void updateProgress(int count) {
        alreadyRead += count;
        double percentage = (double) alreadyRead / (double) contentLength;
        receiver.progress(percentage);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        final int read = super.read(b, off, len);
        if (read > 0) {
            updateProgress(read);
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        super.close();
        receiver.completed();
    }
}