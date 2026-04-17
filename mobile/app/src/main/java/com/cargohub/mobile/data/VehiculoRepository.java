package com.cargohub.mobile.data;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.model.Vehiculo;
import com.cargohub.mobile.data.model.VehiculoUpsertRequest;
import com.cargohub.mobile.network.ApiClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VehiculoRepository {

    private final com.cargohub.mobile.network.ApiService apiService;

    public VehiculoRepository() {
        this(ApiClient.getInstance());
    }

    VehiculoRepository(@NonNull com.cargohub.mobile.network.ApiService apiService) {
        this.apiService = apiService;
    }

    public void getVehiculos(long conductorId, @NonNull RepositoryCallback<List<Vehiculo>> callback) {
        apiService.getVehiculos(conductorId).enqueue(new Callback<List<Vehiculo>>() {
            @Override
            public void onResponse(@NonNull Call<List<Vehiculo>> call,
                                   @NonNull Response<List<Vehiculo>> response) {
                callback.onResult(RepositorySupport.fromResponse(response, "No se pudieron cargar los vehiculos."));
            }

            @Override
            public void onFailure(@NonNull Call<List<Vehiculo>> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al cargar los vehiculos.",
                        "Error de red al cargar los vehiculos."
                ));
            }
        });
    }

    public void createVehiculo(long conductorId,
                               @NonNull VehiculoUpsertRequest request,
                               @NonNull RepositoryCallback<Vehiculo> callback) {
        apiService.createVehiculo(conductorId, request).enqueue(new Callback<Vehiculo>() {
            @Override
            public void onResponse(@NonNull Call<Vehiculo> call, @NonNull Response<Vehiculo> response) {
                callback.onResult(RepositorySupport.fromResponse(response, "No se pudo guardar el vehiculo."));
            }

            @Override
            public void onFailure(@NonNull Call<Vehiculo> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al guardar el vehiculo.",
                        "Error de red al guardar el vehiculo."
                ));
            }
        });
    }

    public void activateVehiculo(long conductorId, long vehiculoId, @NonNull RepositoryCallback<Void> callback) {
        apiService.activateVehiculo(conductorId, vehiculoId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                callback.onResult(RepositorySupport.fromResponse(response, "No se pudo activar el vehiculo."));
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al activar el vehiculo.",
                        "Error de red al activar el vehiculo."
                ));
            }
        });
    }

    public void deactivateVehiculo(long conductorId, long vehiculoId, @NonNull RepositoryCallback<Void> callback) {
        apiService.deactivateVehiculo(conductorId, vehiculoId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                callback.onResult(RepositorySupport.fromResponse(response, "No se pudo desactivar el vehiculo."));
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al desactivar el vehiculo.",
                        "Error de red al desactivar el vehiculo."
                ));
            }
        });
    }
}
