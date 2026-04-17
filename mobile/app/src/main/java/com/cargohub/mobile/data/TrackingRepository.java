package com.cargohub.mobile.data;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.model.DriverLocationUpdateRequest;
import com.cargohub.mobile.network.ApiClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrackingRepository {

    private final com.cargohub.mobile.network.ApiService apiService;

    public TrackingRepository() {
        this(ApiClient.getInstance());
    }

    TrackingRepository(@NonNull com.cargohub.mobile.network.ApiService apiService) {
        this.apiService = apiService;
    }

    public void upsertLocation(long conductorId,
                               @NonNull DriverLocationUpdateRequest request,
                               @NonNull RepositoryCallback<Void> callback) {
        apiService.upsertDriverLocation(conductorId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.code() == 404 || response.code() == 405) {
                    fallbackLegacyLocation(conductorId, request, callback);
                    return;
                }
                callback.onResult(RepositorySupport.fromResponse(response, "No se pudo sincronizar tu ubicacion."));
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al sincronizar la ubicacion.",
                        "Error de red al sincronizar la ubicacion."
                ));
            }
        });
    }

    private void fallbackLegacyLocation(long conductorId,
                                        @NonNull DriverLocationUpdateRequest request,
                                        @NonNull RepositoryCallback<Void> callback) {
        apiService.reportLegacyConductorLocation(conductorId, request.getLat(), request.getLon())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        callback.onResult(RepositorySupport.fromResponse(
                                response,
                                "No se pudo sincronizar tu ubicacion con el endpoint legado."
                        ));
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al sincronizar la ubicacion.",
                                "Error de red al sincronizar la ubicacion."
                        ));
                    }
                });
    }
}
