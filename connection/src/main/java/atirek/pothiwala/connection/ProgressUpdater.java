package atirek.pothiwala.connection;

import androidx.annotation.Nullable;

public class ProgressUpdater implements Runnable {
    private long uploaded;
    private long total;
    private Connector.ProgressListener listener;

    ProgressUpdater(long uploaded, long total, @Nullable Connector.ProgressListener listener) {
        this.uploaded = uploaded;
        this.total = total;
        this.listener = listener;
    }

    @Override
    public void run() {
        int current_percent = (int) (100 * uploaded / total);
        if (listener != null) {
            listener.onUploadProgress(current_percent, 100);
        }
    }
}