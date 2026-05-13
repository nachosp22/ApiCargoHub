package com.cargohub.mobile.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.data.model.LoginResponse;
import com.cargohub.mobile.network.ApiClient;

import java.net.SocketTimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final com.cargohub.mobile.network.ApiService apiService;

    public AuthRepository() {
        this(ApiClient.getInstance());
    }

    AuthRepository(@NonNull com.cargohub.mobile.network.ApiService apiService) {
        this.apiService = apiService;
    }

    public interface LoginCallback {
        void onSuccess(@NonNull LoginResponse loginResponse);

        void onError(@NonNull String message);
    }

    public void login(@NonNull String email, @NonNull String password,
                      @NonNull LoginCallback callback) {
        Call<LoginResponse> call = apiService.login(email, password);

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call,
                                   @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    callback.onSuccess(body);
                } else {
                    String errorBody = parseErrorBody(response);
                    if (response.code() == 401) {
                        if (errorBody != null && errorBody.toLowerCase().contains("inactivo")) {
                            callback.onError("Usuario inactivo");
                        } else {
                            callback.onError("Email o contrasena incorrectos");
                        }
                    } else {
                        callback.onError("No se pudo iniciar sesion. Intenta nuevamente");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado. Revisa tu conexion");
                } else {
                    callback.onError("Error de red. Revisa tu conexion e intenta nuevamente");
                }
            }
        });
    }

    @Nullable
    private String parseErrorBody(@NonNull Response<?> response) {
        try {
            if (response.errorBody() != null) {
                return response.errorBody().string();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
