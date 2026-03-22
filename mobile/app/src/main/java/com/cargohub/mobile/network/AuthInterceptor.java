package com.cargohub.mobile.network;

import androidx.annotation.NonNull;

import com.cargohub.mobile.session.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder();
        String authToken = SessionManager.getAccessToken();
        if (authToken != null && !authToken.trim().isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + authToken.trim());
        }
        return chain.proceed(builder.build());
    }
}
