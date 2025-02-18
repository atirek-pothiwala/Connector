package atirek.pothiwala.connection;

/*
  Created by Atirek Pothiwala on 8/30/2018.
*/

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import atirek.pothiwala.connection.extensions.ConnectListener;
import atirek.pothiwala.connection.extensions.ErrorCode;
import atirek.pothiwala.connection.extensions.ProgressListener;
import atirek.pothiwala.connection.extensions.ProgressUpdater;
import atirek.pothiwala.connection.helpers.Connectivity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class Connector {

    private final Context context;
    private boolean enableDebug;
    private ConnectListener connectListener;
    private ProgressListener progressListener;
    private SwipeRefreshLayout refreshLayout;
    private Dialog loader;

    public Connector(@NonNull Context context) {
        this.context = context;
    }

    /**
     * To enable Debug Mode which will show logs in your Android Studio LOGCAT
     */
    public Connector setDebug(boolean enable) {
        this.enableDebug = enable;
        return this;
    }

    /**
     * Set refresh layout which will be automatically handled.
     */
    public Connector setRefreshLayout(@Nullable SwipeRefreshLayout refreshLayout) {
        this.refreshLayout = refreshLayout;
        return this;
    }

    /**
     * Set listener which will give result and error of your API Calls.
     */
    public Connector setListener(@NonNull ConnectListener listener) {
        this.connectListener = listener;
        return this;
    }

    /**
     * Set listener which will show download progress.
     */
    public Connector setProgressListener(@NonNull ProgressListener listener) {
        this.progressListener = listener;
        return this;
    }

    /**
     * Set a custom loader dialog (Optional), which will be automatically handled.
     */
    public Connector setLoader(@Nullable Dialog loaderDialog) {
        this.loader = loaderDialog;
        return this;
    }

    /**
     * This method can be used to create a RETROFIT CLIENT using a BASE URL
     * Note: You can create your own custom RETROFIT CLIENT.
     */
    public static Retrofit createClient(String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(new OkHttpClient.Builder()
                        .connectTimeout(1, TimeUnit.MINUTES)
                        .writeTimeout(1, TimeUnit.MINUTES)
                        .readTimeout(1, TimeUnit.MINUTES)
                        .build())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
    }

    private void loader(boolean loading) {
        if (loading) {
            if (loader != null && !loader.isShowing()) {
                loader.show();
            }
        } else {
            if (loader != null && loader.isShowing()) {
                loader.dismiss();
            }
            if (refreshLayout != null && refreshLayout.isRefreshing()) {
                refreshLayout.setRefreshing(false);
            }
        }
    }

    private void checkLog(String TAG, Object data) {
        if (enableDebug) {
            Log.d(TAG + ">>", data.toString());
        }
    }

    private Callback<String> createCallback(@NonNull final String TAG) {
        return new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                checkLog(TAG, "Status Code: " + response.code());
                loader(false);

                try {
                    String json;
                    if (response.isSuccessful()) {
                        json = response.body();
                    } else {
                        json = response.errorBody().string();
                    }
                    checkLog(TAG, "Response: " + json);
                    connectListener.onResult(response.code(), json, response.message());
                } catch (Exception e) {
                    checkLog(TAG, "Error: " + e.getMessage());
                    connectListener.onError(ErrorCode.errorSomething);
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                loader(false);

                if (!call.isCanceled()) {
                    checkLog(TAG, "Request Failure");
                    connectListener.onError(ErrorCode.requestFailure);
                } else {
                    checkLog(TAG, "Request Cancelled");
                    connectListener.onError(ErrorCode.requestCancel);
                }
            }
        };
    }

    /**
     * This method can be used to request an API such as GET/POST/PUT/DELETE/UPLOAD.
     */
    public void request(@NonNull final String TAG, @NonNull final Call<String> connect) {
        if (!Connectivity.isInternetAvailable(context)) {
            loader(false);
            connectListener.onError(ErrorCode.internetFailure);
            return;
        }
        loader(true);

        Request request = connect.request();
        RequestBody body = request.body();
        checkLog(TAG, "URL: " + request.url());
        checkLog(TAG, "Params: " + (body != null ? createParams(body) : "Empty"));
        connect.enqueue(createCallback(TAG));
    }

    /**
     * This method can be used to download file as per a specific request of an API.
     */
    public void download(@NonNull final String TAG, @NonNull final Call<ResponseBody> connect) {
        if (!Connectivity.isInternetAvailable(context)) {
            connectListener.onError(ErrorCode.internetFailure);
            return;
        }
        loader(true);

        Request request = connect.request();
        RequestBody body = request.body();
        checkLog(TAG, "URL: " + request.url());
        checkLog(TAG, "Params: " + (body != null ? createParams(body) : "Empty"));

        connect.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull final Response<ResponseBody> response) {
                checkLog(TAG, "Status Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    checkLog(TAG, "File Found");

                    final ExecutorService executorService = Executors.newSingleThreadExecutor();
                    final Handler mainHandler = new Handler(Looper.getMainLooper());

                    Future<String> backgroundProcess = executorService.submit(() -> {
                        checkLog(TAG, "Background Process");

                        File file = createFile(context, request.url().toString());
                        String filePath = Uri.fromFile(file).toString();
                        checkLog(TAG, "File Path: " + filePath);

                        boolean isFileSaved = writeToDisk(file, progressListener, response.body());
                        return isFileSaved ? filePath : null;
                    });
                    executorService.execute(() -> {
                        checkLog(TAG, "Foreground Process");
                        loader(false);
                        try {
                            String filePath = backgroundProcess.get();
                            mainHandler.post(() -> {
                                if (filePath != null) {
                                    connectListener.onResult(response.code(), filePath, response.message());
                                } else {
                                    connectListener.onError(ErrorCode.saveFailure);
                                }
                            });
                        } catch (Exception e) {
                            checkLog(TAG, "Error: " + e.getMessage());
                            connectListener.onError(ErrorCode.errorSomething);
                        } finally {
                            executorService.shutdown();
                        }
                    });
                } else {
                    loader(false);
                    connectListener.onError(ErrorCode.downloadFailure);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                loader(false);

                if (!call.isCanceled()) {
                    checkLog(TAG, "Request Failure: " + throwable.getMessage());
                    connectListener.onError(ErrorCode.downloadFailure);
                } else {
                    checkLog(TAG, "Request Cancelled: " + throwable.getMessage());
                    connectListener.onError(ErrorCode.requestCancel);
                }
            }
        });
    }

    /**
     * This method can be used to cancel running request / download call
     */
    public void cancelCall(Call<?> call) {
        if (call != null && !call.isCanceled() && call.isExecuted()) {
            call.cancel();
        }
    }

    /**
     * To generate empty file with specific extension using url
     */
    private static File createFile(@NonNull Context context, @NonNull String url) {
        File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        return new File(directory, UUID.randomUUID().toString() + "." + MimeTypeMap.getFileExtensionFromUrl(url));
    }

    /**
     * To generate string params with using request body
     */
    private static String createParams(@NonNull RequestBody body) {
        try {
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            return buffer.readUtf8().replace("&", " ");
        } catch (Exception e) {
            return "Unavailable";
        }
    }

    private static String fromStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String newLine = System.getProperty("line.separator", null);
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
            out.append(newLine);
        }
        return out.toString();
    }

    /**
     * To save / write a file in your device and show its progress
     */
    private static boolean writeToDisk(@NonNull File file, @Nullable ProgressListener progressListener, @NonNull ResponseBody body) {
        boolean isWriteResponseBodyToDisk = false;
        try {
            byte[] fileReader = new byte[4096];
            InputStream inputStream = body.byteStream();
            long fileLength = body.contentLength();
            long downloaded = 0;
            OutputStream outputStream = new FileOutputStream(file, false);
            Handler handler = new Handler(Looper.getMainLooper());
            while (true) {
                int read = inputStream.read(fileReader);
                if (read == -1) {
                    break;
                }
                outputStream.write(fileReader, 0, read);
                downloaded = downloaded + read;
                handler.post(new ProgressUpdater(downloaded, fileLength, progressListener));
            }
            outputStream.flush();
            isWriteResponseBodyToDisk = true;

            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isWriteResponseBodyToDisk;
    }
}

