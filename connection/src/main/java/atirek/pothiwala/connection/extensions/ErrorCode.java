package atirek.pothiwala.connection.extensions;

public enum ErrorCode {
    internetFailure("Please check internet connection, try again later."),
    requestFailure("Unable to connect to the server."),
    requestCancel("Request has been cancelled."),
    errorSomething("Something went wrong, please try again."),
    downloadFailure("Unable to download, please try again."),
    saveFailure("Unable to save, please try again.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    String message() {
        return message;
    }
}
