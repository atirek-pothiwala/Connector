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
    private static final String JSON_DATA = "application/json";

    /**
     * Create a request body in FORM-DATA
     * */
    public static RequestBody createPartFromString(@NonNull String content) {
        return RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), content);
    }

    /**
     * Create a request body for a file in FORM-DATA
     * */
    public static MultipartBody.Part createPartFromFile(@NonNull String partName, @NonNull String outputFile, @Nullable ProgressListener listener) {
        File file = new File(outputFile);
        ProgressRequestBody requestFile = new ProgressRequestBody(MediaType.parse(MULTIPART_FORM_DATA), file);
        requestFile.setListener(listener);
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    /**
     * Create a request body in JSON-DATA
     * */
    public static RequestBody createPartFromJson(@NonNull String json) {
        return RequestBody.create(MediaType.parse(JSON_DATA), json);
    }

}
