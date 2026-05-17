package com.cargohub.mobile.data;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.model.CrearFotoCargaRequest;
import com.cargohub.mobile.data.model.FotoCarga;
import com.cargohub.mobile.network.ApiClient;
import com.cargohub.mobile.network.ApiService;

import java.net.SocketTimeoutException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FotoCargaRepository {

    private final ApiService apiService;

    public FotoCargaRepository() {
        this(ApiClient.getInstance());
    }

    FotoCargaRepository(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    public interface FotosCallback {
        void onSuccess(@NonNull List<FotoCarga> fotos);
        void onError(@NonNull String message);
    }

    public interface FotoCallback {
        void onSuccess(@NonNull FotoCarga foto);
        void onError(@NonNull String message);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }

    public void getFotosPorPorte(long porteId, @NonNull FotosCallback callback) {
        apiService.getFotosCarga(porteId).enqueue(new Callback<List<FotoCarga>>() {
            @Override
            public void onResponse(@NonNull Call<List<FotoCarga>> call,
                                   @NonNull Response<List<FotoCarga>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.code() == 401 || response.code() == 403) {
                    callback.onError("No tenes permisos para ver las fotos.");
                } else {
                    callback.onError("No se pudieron cargar las fotos.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<FotoCarga>> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado. Revisa tu conexion.");
                } else {
                    callback.onError("Error de red al cargar fotos.");
                }
            }
        });
    }

    public void subirFoto(long porteId, String tipo, String fotoBase64, String descripcion,
                          @NonNull FotoCallback callback) {
        CrearFotoCargaRequest request = new CrearFotoCargaRequest(tipo, fotoBase64, descripcion);
        apiService.subirFotoCarga(porteId, request).enqueue(new Callback<FotoCarga>() {
            @Override
            public void onResponse(@NonNull Call<FotoCarga> call,
                                   @NonNull Response<FotoCarga> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.code() == 401 || response.code() == 403) {
                    callback.onError("No tenes permisos para subir fotos.");
                } else if (response.code() == 400) {
                    callback.onError("Datos invalidos. Revisa la foto.");
                } else {
                    callback.onError("No se pudo subir la foto.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<FotoCarga> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado. Revisa tu conexion.");
                } else {
                    callback.onError("Error de red al subir la foto.");
                }
            }
        });
    }

    public void eliminarFoto(long porteId, long fotoId, @NonNull DeleteCallback callback) {
        apiService.eliminarFotoCarga(porteId, fotoId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                    return;
                }
                if (response.code() == 401 || response.code() == 403) {
                    callback.onError("No tenes permisos para eliminar esta foto.");
                } else {
                    callback.onError("No se pudo eliminar la foto.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado.");
                } else {
                    callback.onError("Error de red al eliminar la foto.");
                }
            }
        });
    }
}
