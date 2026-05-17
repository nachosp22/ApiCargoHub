package com.cargohub.mobile.data;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.model.ApiErrorResponse;
import com.google.gson.Gson;

import java.net.SocketTimeoutException;
import java.util.List;

import retrofit2.Response;

final class RepositorySupport {

    private static final Gson GSON = new Gson();

    private RepositorySupport() {
    }

    @NonNull
    static <T> RepositoryResult<T> fromResponse(@NonNull Response<T> response,
                                                @NonNull String fallbackMessage) {
        if (response.isSuccessful()) {
            if (response.body() != null) {
                return RepositoryResult.success(response.body());
            }
            return RepositoryResult.successNullable(null, response.code());
        }

        boolean unauthorized = isUnauthorized(response.code());
        return RepositoryResult.error(
                extractMessage(response, fallbackMessage),
                response.code(),
                unauthorized
        );
    }

    static boolean isUnauthorized(int statusCode) {
        return statusCode == 401;
    }

    @NonNull
    static <T> RepositoryResult<T> fromFailure(@NonNull Throwable throwable,
                                               @NonNull String timeoutMessage,
                                               @NonNull String networkMessage) {
        String message = throwable instanceof SocketTimeoutException ? timeoutMessage : networkMessage;
        return RepositoryResult.error(message, -1, false);
    }

    @NonNull
    private static String extractMessage(@NonNull Response<?> response, @NonNull String fallbackMessage) {
        try {
            if (response.errorBody() == null) {
                return fallbackMessage;
            }
            String raw = response.errorBody().string();
            if (raw == null || raw.trim().isEmpty()) {
                return fallbackMessage;
            }
            ApiErrorResponse body = GSON.fromJson(raw, ApiErrorResponse.class);
            if (body != null) {
                List<String> details = body.getDetails();
                if (details != null && !details.isEmpty()) {
                    return details.get(0);
                }
                if (body.getMessage() != null && !body.getMessage().trim().isEmpty()) {
                    return body.getMessage().trim();
                }
            }
            return raw;
        } catch (Exception ignored) {
            return fallbackMessage;
        }
    }
}
