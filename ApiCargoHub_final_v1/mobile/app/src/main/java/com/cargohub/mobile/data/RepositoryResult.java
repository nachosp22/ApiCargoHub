package com.cargohub.mobile.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class RepositoryResult<T> {

    private final T data;
    private final String message;
    private final boolean successful;
    private final boolean unauthorized;
    private final int code;

    private RepositoryResult(@Nullable T data,
                             @NonNull String message,
                             boolean successful,
                             boolean unauthorized,
                             int code) {
        this.data = data;
        this.message = message;
        this.successful = successful;
        this.unauthorized = unauthorized;
        this.code = code;
    }

    public static <T> RepositoryResult<T> success(@NonNull T data) {
        return new RepositoryResult<>(data, "", true, false, 200);
    }

    public static <T> RepositoryResult<T> successNullable(@Nullable T data, int code) {
        return new RepositoryResult<>(data, "", true, false, code);
    }

    public static <T> RepositoryResult<T> error(@NonNull String message, int code, boolean unauthorized) {
        return new RepositoryResult<>(null, message, false, unauthorized, code);
    }

    /**
     * Result served from offline cache. Successful but flagged as cached.
     */
    public static <T> RepositoryResult<T> cached(@NonNull T data) {
        return new RepositoryResult<>(data, "Datos offline (sin conexion)", true, false, -2);
    }

    public boolean isCached() {
        return code == -2;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public boolean isUnauthorized() {
        return unauthorized;
    }

    public int getCode() {
        return code;
    }
}
