package com.cargohub.mobile.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.session.SessionManager;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder();
        String authToken = SessionManager.hasActiveSession() ? SessionManager.getAccessToken() : null;
        if (authToken != null && !authToken.trim().isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + authToken.trim());
        }

        Request authenticatedRequest = builder.build();
        Response response = chain.proceed(authenticatedRequest);
        String responsePreview = null;
        if (response.code() == 401) {
            responsePreview = response.peekBody(2048).string();
        }
        if (shouldInvalidateSession(
                original.url().encodedPath(),
                response.code(),
                authenticatedRequest.header("Authorization") != null,
                responsePreview
        )) {
            SessionManager.handleUnauthorized();
        }
        return response;
    }

    static boolean shouldInvalidateSession(@NonNull String requestPath,
                                           int statusCode,
                                           boolean hasAuthorizationHeader,
                                           @Nullable String responseBody) {
        if (requestPath.contains("/api/auth/login") || statusCode != 401 || !hasAuthorizationHeader) {
            return false;
        }
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return true;
        }
        String body = responseBody.toLowerCase(Locale.ROOT);
        if (body.contains("access denied")
                || body.contains("forbidden")
                || body.contains("no autorizado para ver")) {
            return false;
        }
        return body.contains("unauthorized")
                || body.contains("invalid token")
                || body.contains("jwt")
                || body.contains("token")
                || body.contains("expired")
                || body.contains("credencial");
    }
}
