package com.cargohub.mobile.data;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.network.ApiClient;

import java.net.SocketTimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConductorRepository {

    public interface ProfileCallback {
        void onSuccess(@NonNull ConductorProfileResponse profile);

        void onError(@NonNull String message);
    }

    public void getConductorProfile(long conductorId, @NonNull ProfileCallback callback) {
        Call<ConductorProfileResponse> call = ApiClient.getInstance().getConductorProfile(conductorId);
        call.enqueue(new Callback<ConductorProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ConductorProfileResponse> call,
                                   @NonNull Response<ConductorProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (response.code() == 401 || response.code() == 403) {
                    callback.onError("Tu sesion expiro. Inicia sesion nuevamente.");
                } else if (response.code() == 404) {
                    callback.onError("No encontramos el perfil del conductor.");
                } else {
                    callback.onError("No se pudo cargar tu perfil ahora.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ConductorProfileResponse> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado. Revisa tu conexion.");
                } else {
                    callback.onError("Error de red al cargar tu perfil.");
                }
            }
        });
    }
}
