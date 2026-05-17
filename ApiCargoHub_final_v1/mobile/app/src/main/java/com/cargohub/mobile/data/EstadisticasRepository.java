package com.cargohub.mobile.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.data.model.EstadisticasConductor;
import com.cargohub.mobile.network.ApiClient;
import com.cargohub.mobile.network.ApiService;

import java.net.SocketTimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EstadisticasRepository {

    private final ApiService apiService;

    public EstadisticasRepository() {
        this(ApiClient.getInstance());
    }

    EstadisticasRepository(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    public interface EstadisticasCallback {
        void onSuccess(@NonNull EstadisticasConductor estadisticas);
        void onError(@NonNull String message);
    }

    public void getEstadisticas(long conductorId, @Nullable String desde, @Nullable String hasta,
                                @NonNull EstadisticasCallback callback) {
        apiService.getEstadisticas(conductorId, desde, hasta)
                .enqueue(new Callback<EstadisticasConductor>() {
                    @Override
                    public void onResponse(@NonNull Call<EstadisticasConductor> call,
                                           @NonNull Response<EstadisticasConductor> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                            return;
                        }
                        if (response.code() == 401 || response.code() == 403) {
                            callback.onError(response.code() == 401
                                    ? "Tu sesion expiro. Inicia sesion nuevamente."
                                    : "No tenes permisos para ver estas estadisticas.");
                        } else {
                            callback.onError("No se pudieron cargar las estadisticas.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<EstadisticasConductor> call, @NonNull Throwable t) {
                        if (t instanceof SocketTimeoutException) {
                            callback.onError("Tiempo de espera agotado. Revisa tu conexion.");
                        } else {
                            callback.onError("Error de red al cargar estadisticas.");
                        }
                    }
                });
    }
}
