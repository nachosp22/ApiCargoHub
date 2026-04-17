package com.cargohub.mobile.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.data.model.Factura;
import com.cargohub.mobile.data.model.FacturaPageResponse;
import com.cargohub.mobile.data.model.FacturaResumen;
import com.cargohub.mobile.network.ApiClient;
import com.cargohub.mobile.network.ApiService;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FacturaRepository {

    private final ApiService apiService;

    public FacturaRepository() {
        this(ApiClient.getInstance());
    }

    FacturaRepository(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    public interface FacturasCallback {
        void onSuccess(@NonNull List<Factura> facturas, int totalPages);
        void onError(@NonNull String message);
    }

    public interface ResumenCallback {
        void onSuccess(@NonNull FacturaResumen resumen);
        void onError(@NonNull String message);
    }

    public void getFacturas(long conductorId, @Nullable String desde, @Nullable String hasta,
                            @Nullable Boolean pagada, int page, int size,
                            @NonNull FacturasCallback callback) {
        apiService.getFacturas(conductorId, desde, hasta, pagada, page, size)
                .enqueue(new Callback<FacturaPageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<FacturaPageResponse> call,
                                           @NonNull Response<FacturaPageResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            FacturaPageResponse body = response.body();
                            List<Factura> content = body.getContent() != null
                                    ? body.getContent() : Collections.emptyList();
                            callback.onSuccess(content, body.getTotalPages());
                            return;
                        }
                        if (response.code() == 401 || response.code() == 403) {
                            callback.onError(authError(response.code()));
                        } else {
                            callback.onError("No se pudieron cargar las facturas.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<FacturaPageResponse> call, @NonNull Throwable t) {
                        callback.onError(networkError(t));
                    }
                });
    }

    public void getResumen(long conductorId, @Nullable String periodo,
                           @NonNull ResumenCallback callback) {
        apiService.getFacturasResumen(conductorId, periodo)
                .enqueue(new Callback<FacturaResumen>() {
                    @Override
                    public void onResponse(@NonNull Call<FacturaResumen> call,
                                           @NonNull Response<FacturaResumen> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                            return;
                        }
                        if (response.code() == 401 || response.code() == 403) {
                            callback.onError(authError(response.code()));
                        } else {
                            callback.onError("No se pudo cargar el resumen de facturacion.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<FacturaResumen> call, @NonNull Throwable t) {
                        callback.onError(networkError(t));
                    }
                });
    }

    private String authError(int code) {
        return code == 401 ? "Tu sesion expiro. Inicia sesion nuevamente."
                : "No tenes permisos para ver estas facturas.";
    }

    private String networkError(Throwable t) {
        return t instanceof SocketTimeoutException
                ? "Tiempo de espera agotado. Revisa tu conexion."
                : "Error de red al cargar facturas.";
    }
}
