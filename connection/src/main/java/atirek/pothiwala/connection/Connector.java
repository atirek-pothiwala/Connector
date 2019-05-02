package atirek.pothiwala.connection;

/**
 * Created by Atirek Pothiwala on 8/30/2018.
 */

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static atirek.pothiwala.connection.Connector.ErrorText.checkInternet;
import static atirek.pothiwala.connection.Connector.ErrorText.errorSomething;
import static atirek.pothiwala.connection.Connector.ErrorText.failureConnect;
import static atirek.pothiwala.connection.Connector.ErrorText.failureDownload;
import static atirek.pothiwala.connection.Connector.ErrorText.failureSave;
import static atirek.pothiwala.connection.Connector.ErrorText.failureUpload;

public class Connector {

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private Context context;
    private ConnectListener listener;
    private SwipeRefreshLayout refreshLayout;
    private Dialog loaderDialog;
    private boolean enableDebug;

    public interface ConnectListener {
        void onSuccess(@NonNull String TAG, @Nullable String json, @NonNull Headers headers);

        void onFailure(@NonNull String TAG, boolean isNetworkIssue, @Nullable String errorMessage);
    }

    public interface ErrorText {
        String checkInternet = "This field cannot be empty.";
        String failureConnect = "Unable to connect to the server.";
        String errorSomething = "Something went wrong, please try again.";
        String failureUpload = "Unable to upload, please try again.";
        String failureDownload = "Unable to download, please try again.";
        String failureSave = "Unable to save, please try again.";
    }

    public Connector(@NonNull Context context, boolean enableDebug) {
        this.context = context;
        this.enableDebug = enableDebug;
    }

    public void setRefreshLayout(@NonNull SwipeRefreshLayout refreshLayout) {
        this.refreshLayout = refreshLayout;
    }

    public void setListener(@NonNull ConnectListener listener) {
        this.listener = listener;
    }

    public void setLoaderDialog(@Nullable Dialog loaderDialog) {
        this.loaderDialog = loaderDialog;
    }

    public static Retrofit getClient(String base_url) {
        return new Retrofit.Builder()
                .baseUrl(base_url)
                .client(new OkHttpClient.Builder()
                        .connectTimeout(1, TimeUnit.MINUTES)
                        .writeTimeout(1, TimeUnit.MINUTES)
                        .readTimeout(1, TimeUnit.MINUTES)
                        .build())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
    }

    @SuppressLint("MissingPermission")
    public static boolean isNoInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
        }
        return true;
    }

    private void enableLoader(boolean loading) {
        if (loading) {

            if (loaderDialog != null && !loaderDialog.isShowing()) {
                loaderDialog.show();
            }
        } else {
            if (loaderDialog != null && loaderDialog.isShowing()) {
                loaderDialog.dismiss();
            }

            if (refreshLayout != null && refreshLayout.isRefreshing()) {
                refreshLayout.setRefreshing(false);
            }
        }
    }

    public void Request(@NonNull final String TAG, @NonNull final Call<String> connect) {

        if (isNoInternet(context)) {
            enableLoader(false);
            listener.onFailure(TAG, true, checkInternet);
            return;
        }

        enableLoader(true);

        checkLog(TAG, "URL: " + connect.request().url());
        checkLog(TAG, "Params: " + getParams(connect.request().body()));

        connect.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {

                enableLoader(false);

                if (!call.isCanceled()) {
                    checkLog(TAG, "Response: " + response.body());
                    listener.onSuccess(TAG, response.body(), response.headers());
                } else {
                    checkLog(TAG, "Request Cancelled");
                    listener.onFailure(TAG, false, "");
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {

                enableLoader(false);

                if (!call.isCanceled()) {
                    checkLog(TAG, "Request Failure");
                    listener.onFailure(TAG, false, failureConnect);
                } else {
                    checkLog(TAG, "Request Cancelled");
                    listener.onFailure(TAG, false, "");
                }
            }
        });
    }

    public void Upload(@NonNull final String TAG, @NonNull Call<ResponseBody> connect) {

        if (isNoInternet(context)) {
            listener.onFailure(TAG, true, checkInternet);
            return;
        }

        enableLoader(true);

        checkLog(TAG, "URL: " + connect.request().url());
        checkLog(TAG, "Params: " + getParams(connect.request().body()));

        connect.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

                enableLoader(false);

                try {
                    if (!call.isCanceled()) {
                        checkLog(TAG, "Response: " + response.body());
                        listener.onSuccess(TAG, fromStream(response.body().byteStream()), response.headers());
                    } else {
                        checkLog(TAG, "Request Cancelled");
                        listener.onFailure(TAG, false, "");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    checkLog(TAG, "Exception: " + e.getMessage());
                    listener.onFailure(TAG, false, errorSomething);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {

                enableLoader(false);

                if (!call.isCanceled()) {
                    checkLog(TAG, "Request Failure");
                    listener.onFailure(TAG, false, throwable.getMessage());
                } else {
                    checkLog(TAG, "Request Cancelled");
                    listener.onFailure(TAG, false, failureUpload);
                }
            }
        });

    }

    public void Download(@NonNull final String TAG, @NonNull final Call<ResponseBody> connect) {

        if (isNoInternet(context)) {
            listener.onFailure(TAG, true, checkInternet);
            return;
        }

        enableLoader(true);

        checkLog(TAG, "URL: " + connect.request().url());
        checkLog(TAG, "Params: " + getParams(connect.request().body()));

        connect.enqueue(new Callback<ResponseBody>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull final Response<ResponseBody> response) {


                if (!call.isCanceled()) {

                    if (response.isSuccessful() && response.body() != null) {
                        checkLog(TAG, "File Found");

                        new AsyncTask<Boolean, Void, Boolean>() {

                            String filePath;

                            @Override
                            protected Boolean doInBackground(Boolean... bools) {

                                checkLog(TAG, "doInBackground");

                                File file = getFile(context, connect.request().url().toString());
                                filePath = Uri.fromFile(file).toString();
                                checkLog(TAG, "FilePath: " + filePath);
                                return writeResponseBodyToDisk(file, response.body());
                            }

                            @Override
                            protected void onPostExecute(Boolean isSaved) {
                                super.onPostExecute(isSaved);

                                checkLog(TAG, "onPostExecute");
                                enableLoader(false);

                                if (isSaved) {
                                    listener.onSuccess(TAG, filePath, response.headers());
                                } else {
                                    listener.onFailure(TAG, false, failureSave);
                                }

                            }
                        }.execute();

                    } else {
                        enableLoader(false);

                        checkLog(TAG, failureDownload);
                        listener.onFailure(TAG, false, failureDownload);
                    }

                } else {
                    enableLoader(false);

                    checkLog(TAG, "Request Cancelled");
                    listener.onFailure(TAG, false, "");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {

                enableLoader(false);

                if (!call.isCanceled()) {
                    checkLog(TAG, "Request Failure: " + throwable.getMessage());
                    listener.onFailure(TAG, false, failureDownload);
                } else {
                    checkLog(TAG, "Request Cancelled: " + throwable.getMessage());
                    listener.onFailure(TAG, false, "");
                }
            }
        });

    }

    public void cancelCall(Call<?> call) {
        if (call != null && !call.isCanceled() && call.isExecuted()) {
            call.cancel();
        }
    }

    private void checkLog(String TAG, Object data) {
        if (enableDebug) {
            Log.d(TAG + ">>", data.toString());
        }
    }

    @Nullable
    public static String getMacAddress() {
        try {
            List<NetworkInterface> interfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaceList) {

                if (!networkInterface.getName().equalsIgnoreCase("wlan0")) {
                    continue;
                }

                byte[] macBytes = networkInterface.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder stringBuilder = new StringBuilder();
                for (byte b : macBytes) {
                    stringBuilder.append(Integer.toHexString(b & 0xFF)).append(":");
                }

                if (stringBuilder.length() > 0) {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }
                return stringBuilder.toString();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
        //return "02:00:00:00:00:00";
    }

    private static File getFile(Context context, String url) {
        File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        return new File(directory, UUID.randomUUID().toString() + "." + MimeTypeMap.getFileExtensionFromUrl(url));
    }

    private static String getParams(RequestBody request) {
        try {
            Buffer buffer = new Buffer();
            if (request != null) {
                request.writeTo(buffer);
            } else {
                return "";
            }
            return buffer.readUtf8().replace("&", " ");
        } catch (IOException e) {
            return "Unavailable";
        }
    }

    private static String fromStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
            out.append(newLine);
        }
        return out.toString();
    }

    private boolean writeResponseBodyToDisk(File saveFile, ResponseBody body) {
        try {

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                //long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(saveFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded = fileSizeDownloaded + read;
                    //Utils.checkLog(TAG, "Download: " + fileSizeDownloaded + " of " + fileSize, null);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    public static RequestBody createPartFromString(String descriptionString) {
        if (descriptionString == null) {
            descriptionString = "";
        }
        return RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), descriptionString);
    }

    public static MultipartBody.Part prepareFilePart(String partName, String outputFile) {
        File file = new File(outputFile);
        RequestBody requestFile = RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), file);
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    public static RequestBody createPartFromJsonObject(String data) {
        return RequestBody.create(MediaType.parse("application/json"), data);
    }

}
