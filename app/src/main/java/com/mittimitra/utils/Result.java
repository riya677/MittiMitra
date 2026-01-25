package com.mittimitra.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A wrapper class for representing the result of an operation that might fail.
 * Inspired by Kotlin's Result class - provides a clean way to handle success/failure states.
 * 
 * @param <T> The type of the success value
 */
public class Result<T> {
    
    @Nullable
    private final T data;
    
    @Nullable
    private final Throwable error;
    
    @Nullable
    private final String message;
    
    private final boolean success;

    private Result(@Nullable T data, @Nullable Throwable error, @Nullable String message, boolean success) {
        this.data = data;
        this.error = error;
        this.message = message;
        this.success = success;
    }

    /**
     * Create a successful result with data.
     */
    @NonNull
    public static <T> Result<T> success(@NonNull T data) {
        return new Result<>(data, null, null, true);
    }

    /**
     * Create a successful result with data and a message.
     */
    @NonNull
    public static <T> Result<T> success(@NonNull T data, @Nullable String message) {
        return new Result<>(data, null, message, true);
    }

    /**
     * Create a failure result with an error.
     */
    @NonNull
    public static <T> Result<T> failure(@NonNull Throwable error) {
        return new Result<>(null, error, error.getMessage(), false);
    }

    /**
     * Create a failure result with a message.
     */
    @NonNull
    public static <T> Result<T> failure(@NonNull String message) {
        return new Result<>(null, null, message, false);
    }

    /**
     * Create a failure result with both error and custom message.
     */
    @NonNull
    public static <T> Result<T> failure(@NonNull Throwable error, @NonNull String message) {
        return new Result<>(null, error, message, false);
    }

    /**
     * Create a loading/in-progress result.
     */
    @NonNull
    public static <T> Result<T> loading() {
        return new Result<>(null, null, "Loading...", true);
    }

    // ========== GETTERS ==========

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    @Nullable
    public T getData() {
        return data;
    }

    /**
     * Get data or throw exception if failed.
     */
    @NonNull
    public T getDataOrThrow() throws RuntimeException {
        if (data == null) {
            throw new RuntimeException(message != null ? message : "No data available");
        }
        return data;
    }

    /**
     * Get data or return a default value if failed.
     */
    @NonNull
    public T getDataOrDefault(@NonNull T defaultValue) {
        return data != null ? data : defaultValue;
    }

    @Nullable
    public Throwable getError() {
        return error;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    // ========== FUNCTIONAL OPERATIONS ==========

    /**
     * Transform the success value using the provided function.
     */
    @NonNull
    public <R> Result<R> map(@NonNull Function<T, R> mapper) {
        if (success && data != null) {
            try {
                return Result.success(mapper.apply(data));
            } catch (Exception e) {
                return Result.failure(e);
            }
        }
        return Result.failure(error != null ? error : new RuntimeException(message));
    }

    /**
     * Execute action on success.
     */
    @NonNull
    public Result<T> onSuccess(@NonNull Consumer<T> action) {
        if (success && data != null) {
            action.accept(data);
        }
        return this;
    }

    /**
     * Execute action on failure.
     */
    @NonNull
    public Result<T> onFailure(@NonNull Consumer<Throwable> action) {
        if (!success && error != null) {
            action.accept(error);
        }
        return this;
    }

    /**
     * Functional interface for transforming values.
     */
    public interface Function<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * Functional interface for consuming values.
     */
    public interface Consumer<T> {
        void accept(T t);
    }
}
