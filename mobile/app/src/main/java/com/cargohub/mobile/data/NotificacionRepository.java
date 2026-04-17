package com.cargohub.mobile.data;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.model.Notificacion;
import com.cargohub.mobile.data.model.UnreadCountResponse;
import com.cargohub.mobile.network.ApiClient;
import com.cargohub.mobile.network.ApiService;

import java.net.SocketTimeoutException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificacionRepository {

    private final ApiService apiService;

    public NotificacionRepository() {
        this(ApiClient.getInstance());
    }

    NotificacionRepository(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    public interface NotificacionesCallback {
        void onSuccess(@NonNull List<Notificacion> notificaciones);
        void onError(@NonNull String message);
    }

    public interface UnreadCountCallback {
        void onSuccess(long count);
        void onError(@NonNull String message);
    }

    public interface MarcarLeidaCallback {
        void onSuccess(@NonNull Notificacion notificacion);
        void onError(@NonNull String message);
    }

    public interface MarcarTodasCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }

    public void getNotificaciones(@NonNull NotificacionesCallback callback) {
        apiService.getNotificaciones().enqueue(new Callback<List<Notificacion>>() {
            @Override
            public void onResponse(@NonNull Call<List<Notificacion>> call,
                                   @NonNull Response<List<Notificacion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.code() == 401 || response.code() == 403) {
                    callback.onError(messageForAuthError(response.code()));
                } else {
                    callback.onError("No se pudieron cargar las notificaciones.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Notificacion>> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    callback.onError("Tiempo de espera agotado. Revisa tu conexion.");
                } else {
                    callback.onError("Error de red al cargar notificaciones.");
                }
            }
        });
    }

    public void getUnreadCount(@NonNull UnreadCountCallback callback) {
        apiService.getNotificacionesUnreadCount().enqueue(new Callback<UnreadCountResponse>() {
            @Override
            public void onResponse(@NonNull Call<UnreadCountResponse> call,
                                   @NonNull Response<UnreadCountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().getUnread());
                    return;
                }
                callback.onError("No se pudo obtener el conteo.");
            }

            @Override
            public void onFailure(@NonNull Call<UnreadCountResponse> call, @NonNull Throwable t) {
                callback.onError("Error de red.");
            }
        });
    }

    public void markAsRead(long notificacionId, @NonNull MarcarLeidaCallback callback) {
        apiService.markNotificacionAsRead(notificacionId).enqueue(new Callback<Notificacion>() {
            @Override
            public void onResponse(@NonNull Call<Notificacion> call,
                                   @NonNull Response<Notificacion> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                    return;
                }
                if (response.code() == 401 || response.code() == 403) {
                    callback.onError(messageForAuthError(response.code()));
                } else {
                    callback.onError("No se pudo marcar como leida.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Notificacion> call, @NonNull Throwable t) {
                callback.onError("Error de red.");
            }
        });
    }

    public void markAllAsRead(@NonNull MarcarTodasCallback callback) {
        apiService.markAllNotificacionesAsRead().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                    return;
                }
                callback.onError("No se pudieron marcar como leidas.");
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError("Error de red.");
            }
        });
    }

    @NonNull
    private String messageForAuthError(int code) {
        if (code == 401) {
            return "Tu sesion expiro. Inicia sesion nuevamente.";
        }
        return "No tenes permisos para acceder a las notificaciones.";
    }
}
