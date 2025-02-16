package atirek.pothiwala.connection.extensions;

import androidx.annotation.Nullable;

public class ProgressUpdater implements Runnable {

    private final long uploaded;
    private final long total;
    private final ProgressListener listener;

    public ProgressUpdater(long uploaded, long total, @Nullable ProgressListener listener) {
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
