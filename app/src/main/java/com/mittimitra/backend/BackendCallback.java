package com.mittimitra.backend;

import androidx.annotation.NonNull;

public interface BackendCallback<T> {
    void onSuccess(@NonNull ApiEnvelope<T> envelope);

    void onFailure(@NonNull ApiEnvelope<T> envelope, Throwable throwable);
}
