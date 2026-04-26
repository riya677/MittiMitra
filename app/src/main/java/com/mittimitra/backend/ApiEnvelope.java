package com.mittimitra.backend;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ApiEnvelope<T> {
    public String status;
    public String code;
    public String message;
    public T data;
    public String traceId;

    public ApiEnvelope() {
    }

    public static <T> ApiEnvelope<T> success(@Nullable T data) {
        ApiEnvelope<T> envelope = new ApiEnvelope<>();
        envelope.status = "success";
        envelope.code = "OK";
        envelope.message = "Success";
        envelope.data = data;
        return envelope;
    }

    public static <T> ApiEnvelope<T> error(@NonNull String code, @NonNull String message, @Nullable String traceId) {
        ApiEnvelope<T> envelope = new ApiEnvelope<>();
        envelope.status = "error";
        envelope.code = code;
        envelope.message = message;
        envelope.traceId = traceId;
        return envelope;
    }

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
