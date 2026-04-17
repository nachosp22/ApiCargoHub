package com.cargohub.mobile.data;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.model.EstadoIncidencia;
import com.cargohub.mobile.data.model.CrearIncidenciaRequest;
import com.cargohub.mobile.data.model.IncidenciaEventoResponse;
import com.cargohub.mobile.data.model.IncidenciaResponse;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.network.ApiClient;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidenciaRepository {

    private final com.cargohub.mobile.network.ApiService apiService;

    public IncidenciaRepository() {
        this(ApiClient.getInstance());
    }

    IncidenciaRepository(@NonNull com.cargohub.mobile.network.ApiService apiService) {
        this.apiService = apiService;
    }

    public interface PortesCallback {
        void onSuccess(@NonNull List<Porte> portes);
        void onError(@NonNull String message);
    }

    public interface IncidenciasCallback {
        void onSuccess(@NonNull List<IncidenciaResponse> incidencias);
        void onError(@NonNull String message);
    }

    public interface IncidenciaDetalleCallback {
        void onSuccess(@NonNull IncidenciaResponse incidencia);
        void onError(@NonNull String message);
    }

    public interface HistorialCallback {
        void onSuccess(@NonNull List<IncidenciaEventoResponse> historial);
        void onError(@NonNull String message);
    }

    public interface CrearCallback {
        void onSuccess(@NonNull IncidenciaResponse incidencia);
        void onError(@NonNull String message);
    }

    public void getPortesDelConductor(long conductorId, @NonNull PortesCallback callback) {
        Call<List<Porte>> call = apiService.getPortesDelConductor(conductorId);
        call.enqueue(new Callback<List<Porte>>() {
            @Override
            public void onResponse(@NonNull Call<List<Porte>> call,
                                   @NonNull Response<List<Porte>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (response.code() == 401 || response.code() == 403) {
                    callback.onError(messageForAuthError(response.code()));
                } else {
                    callback.onError("No se pudieron cargar tus portes ahora.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Porte>> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado. Revisa tu conexion.");
                } else {
                    callback.onError("Error de red al cargar portes.");
                }
            }
        });
    }

    public void getActivas(long conductorId, @NonNull IncidenciasCallback callback) {
        EnumSet<EstadoIncidencia> estadosActivos = EnumSet.of(
                EstadoIncidencia.ABIERTA,
                EstadoIncidencia.EN_REVISION
        );
        fetchIncidenciasPorConductor(conductorId, estadosActivos, callback);
    }

    public void getHistorial(long conductorId, @NonNull IncidenciasCallback callback) {
        EnumSet<EstadoIncidencia> estadosResueltos = EnumSet.of(
                EstadoIncidencia.RESUELTA,
                EstadoIncidencia.DESESTIMADA
        );
        fetchIncidenciasPorConductor(conductorId, estadosResueltos, callback);
    }

    public void getPorPorte(long porteId, @NonNull IncidenciasCallback callback) {
        apiService.getIncidenciasPorPorte(porteId).enqueue(new Callback<List<IncidenciaResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<IncidenciaResponse>> call,
                                   @NonNull Response<List<IncidenciaResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (response.code() == 401 || response.code() == 403) {
                    callback.onError(messageForAuthError(response.code()));
                } else {
                    callback.onError("No se pudo cargar el historial de incidencias del porte.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<IncidenciaResponse>> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado. Revisa tu conexion.");
                } else {
                    callback.onError("Error de red al cargar incidencias del porte.");
                }
            }
        });
    }

    private void fetchIncidenciasPorConductor(long conductorId,
                                               EnumSet<EstadoIncidencia> estadosDeseados,
                                               @NonNull IncidenciasCallback callback) {
        Call<List<Porte>> portesCall = apiService.getPortesDelConductor(conductorId);
        portesCall.enqueue(new Callback<List<Porte>>() {
            @Override
            public void onResponse(@NonNull Call<List<Porte>> call,
                                   @NonNull Response<List<Porte>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (response.code() == 401 || response.code() == 403) {
                        callback.onError(messageForAuthError(response.code()));
                    } else {
                        callback.onError("No se pudieron cargar tus portes ahora.");
                    }
                    return;
                }

                List<Porte> portes = response.body();
                if (portes.isEmpty()) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                final List<IncidenciaResponse> todasIncidencias = new ArrayList<>();
                final int[] pendingCalls = {portes.size()};

                for (Porte porte : portes) {
                    if (porte.getId() == null) {
                        pendingCalls[0]--;
                        continue;
                    }
                    Call<List<IncidenciaResponse>> incidenciasCall =
                            apiService.getIncidenciasPorPorte(porte.getId());
                    incidenciasCall.enqueue(new Callback<List<IncidenciaResponse>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<IncidenciaResponse>> call,
                                               @NonNull Response<List<IncidenciaResponse>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                synchronized (todasIncidencias) {
                                    todasIncidencias.addAll(response.body());
                                }
                            }
                            pendingCalls[0]--;
                            if (pendingCalls[0] == 0) {
                                List<IncidenciaResponse> filtradas = filtrarPorEstados(
                                        todasIncidencias, estadosDeseados);
                                callback.onSuccess(filtradas);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<List<IncidenciaResponse>> call,
                                             @NonNull Throwable t) {
                            pendingCalls[0]--;
                            if (pendingCalls[0] == 0) {
                                List<IncidenciaResponse> filtradas = filtrarPorEstados(
                                        todasIncidencias, estadosDeseados);
                                callback.onSuccess(filtradas);
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Porte>> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado. Revisa tu conexion.");
                } else {
                    callback.onError("Error de red al cargar portes.");
                }
            }
        });
    }

    private List<IncidenciaResponse> filtrarPorEstados(List<IncidenciaResponse> incidencias,
                                                        EnumSet<EstadoIncidencia> estados) {
        List<IncidenciaResponse> filtradas = new ArrayList<>();
        for (IncidenciaResponse incidencia : incidencias) {
            if (incidencia.getEstado() != null && estados.contains(incidencia.getEstado())) {
                filtradas.add(incidencia);
            }
        }
        return filtradas;
    }

    public void crearIncidencia(long porteId, String titulo, String descripcion,
                                   String severidad, String prioridad, @NonNull CrearCallback callback) {
        Call<IncidenciaResponse> call = apiService.crearIncidencia(
                porteId,
                new CrearIncidenciaRequest(titulo, descripcion, severidad, prioridad)
        );
        call.enqueue(new Callback<IncidenciaResponse>() {
            @Override
            public void onResponse(@NonNull Call<IncidenciaResponse> call,
                                   @NonNull Response<IncidenciaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }

                if (response.code() == 401 || response.code() == 403) {
                    callback.onError(messageForAuthError(response.code()));
                } else if (response.code() == 400) {
                    callback.onError("Datos invalidos. Revisa el formulario.");
                } else {
                    callback.onError("No se pudo crear la incidencia. Intenta mas tarde.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<IncidenciaResponse> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado. Revisa tu conexion.");
                } else {
                    callback.onError("Error de red al crear la incidencia.");
                }
            }
        });
    }

    public void getPorId(long incidenciaId, @NonNull IncidenciaDetalleCallback callback) {
        apiService.getIncidenciaById(incidenciaId).enqueue(new Callback<IncidenciaResponse>() {
            @Override
            public void onResponse(@NonNull Call<IncidenciaResponse> call,
                                   @NonNull Response<IncidenciaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.code() == 401 || response.code() == 403) {
                    callback.onError(messageForAuthError(response.code()));
                } else if (response.code() == 404) {
                    callback.onError("No encontramos la incidencia seleccionada.");
                } else {
                    callback.onError("No se pudo cargar el detalle de la incidencia.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<IncidenciaResponse> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado. Revisa tu conexion.");
                } else {
                    callback.onError("Error de red al cargar la incidencia.");
                }
            }
        });
    }

    public void getHistorialIncidencia(long incidenciaId, @NonNull HistorialCallback callback) {
        apiService.getIncidenciaHistorial(incidenciaId).enqueue(new Callback<List<IncidenciaEventoResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<IncidenciaEventoResponse>> call,
                                   @NonNull Response<List<IncidenciaEventoResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.code() == 401 || response.code() == 403) {
                    callback.onError(messageForAuthError(response.code()));
                } else {
                    callback.onError("No se pudo cargar el historial de cambios.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<IncidenciaEventoResponse>> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado. Revisa tu conexion.");
                } else {
                    callback.onError("Error de red al cargar el historial.");
                }
            }
        });
    }

    @NonNull
    private String messageForAuthError(int code) {
        if (code == 401) {
            return "Tu sesion expiro. Inicia sesion nuevamente.";
        }
        return "No tenes permisos para acceder a esta incidencia.";
    }
}
