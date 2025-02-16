package atirek.pothiwala.connection.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import atirek.pothiwala.connection.extensions.ProgressListener;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class RequestCreator {

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    public static RequestBody createPartFromString(@NonNull String content) {
        return RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), content);
    }

    public static MultipartBody.Part prepareFilePart(@NonNull String partName, @NonNull String outputFile, @Nullable ProgressListener listener) {
        File file = new File(outputFile);
        ProgressRequestBody requestFile = new ProgressRequestBody(MediaType.parse(MULTIPART_FORM_DATA), file);
        requestFile.setListener(listener);
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    public static RequestBody createPartFromJsonObject(@NonNull String data) {
        return RequestBody.create(MediaType.parse("application/json"), data);
    }

}
