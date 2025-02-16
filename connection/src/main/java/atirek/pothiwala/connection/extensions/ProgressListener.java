package atirek.pothiwala.connection.extensions;

public interface ProgressListener {
    void onUploadProgress(int currentPercent, int totalPercent);
}