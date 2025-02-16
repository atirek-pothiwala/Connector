package atirek.pothiwala.connection.extensions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import atirek.pothiwala.connection.Connector;

public interface ConnectListener {
    void onResult(int statusCode, @Nullable String data, @Nullable String message);

    void onError(@NonNull Connector.ErrorCode code);
}
