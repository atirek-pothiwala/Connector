package atirek.pothiwala.connection;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ProgressRequestBody extends RequestBody {

    private File file;
    private static final int DEFAULT_BUFFER_SIZE = 2048;

    private Connector.ProgressListener listener;
    private MediaType mediaType;

    ProgressRequestBody(@Nullable MediaType mediaType, @NonNull File file) {
        this.file = file;
        this.mediaType = mediaType;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    void setListener(@Nullable Connector.ProgressListener listener) {
        this.listener = listener;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
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

