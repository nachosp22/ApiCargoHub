package com.cargohub.mobile.data;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.data.model.ConductorProfileUpdateRequest;
import com.cargohub.mobile.network.ApiClient;

import java.net.SocketTimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConductorRepository {

    private final com.cargohub.mobile.network.ApiService apiService;

    public ConductorRepository() {
        this(ApiClient.getInstance());
    }

    ConductorRepository(@NonNull com.cargohub.mobile.network.ApiService apiService) {
        this.apiService = apiService;
    }

    public interface ProfileCallback {
        void onSuccess(@NonNull ConductorProfileResponse profile);

        void onError(@NonNull String message);
    }

    public void getConductorProfile(long conductorId, @NonNull ProfileCallback callback) {
        Call<ConductorProfileResponse> call = apiService.getConductorProfile(conductorId);
        call.enqueue(new Callback<ConductorProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ConductorProfileResponse> call,
                                   @NonNull Response<ConductorProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (response.code() == 401 || response.code() == 403) {
                    callback.onError(response.code() == 401
                            ? "Tu sesion expiro. Inicia sesion nuevamente."
                            : "No tenes permisos para ver este perfil.");
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

    public void updateConductorProfile(long conductorId,
                                       @NonNull ConductorProfileUpdateRequest request,
                                       @NonNull RepositoryCallback<ConductorProfileResponse> callback) {
        apiService.updateConductorProfile(conductorId, request)
                .enqueue(new Callback<ConductorProfileResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ConductorProfileResponse> call,
                                           @NonNull Response<ConductorProfileResponse> response) {
                        callback.onResult(RepositorySupport.fromResponse(response, "No se pudo actualizar tu perfil."));
                    }

                    @Override
                    public void onFailure(@NonNull Call<ConductorProfileResponse> call, @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al guardar tu perfil.",
                                "Error de red al guardar tu perfil."
                        ));
                    }
                });
    }

    public void deactivateConductor(long conductorId, @NonNull RepositoryCallback<Void> callback) {
        apiService.deactivateConductorProfile(conductorId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        callback.onResult(RepositorySupport.fromResponse(response, "No se pudo desactivar tu cuenta."));
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al desactivar tu cuenta.",
                                "Error de red al desactivar tu cuenta."
                        ));
                    }
                });
    }
}
