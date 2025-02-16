package atirek.pothiwala.connection.helpers;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import atirek.pothiwala.connection.extensions.ProgressListener;
import atirek.pothiwala.connection.extensions.ProgressUpdater;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ProgressRequestBody extends RequestBody {

    private final MediaType mediaType;
    private final File file;
    private static final int DEFAULT_BUFFER_SIZE = 2048;

    private ProgressListener listener;

    public ProgressRequestBody(@Nullable MediaType mediaType, @NonNull File file) {
        this.mediaType = mediaType;
        this.file = file;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    public void setListener(@Nullable ProgressListener listener) {
        this.listener = listener;
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        long fileLength = file.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        FileInputStream in = new FileInputStream(file);
        long uploaded = 0;

        try {
            int read;
            Handler handler = new Handler(Looper.getMainLooper());
            while ((read = in.read(buffer)) != -1) {

                // update progress on UI thread
                handler.post(new ProgressUpdater(uploaded, fileLength, listener));
                uploaded += read;
                sink.write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
    }
}

