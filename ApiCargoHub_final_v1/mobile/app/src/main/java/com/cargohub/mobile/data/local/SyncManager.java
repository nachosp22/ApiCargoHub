package com.cargohub.mobile.data.local;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.local.dao.PendingOperationDao;
import com.cargohub.mobile.data.local.entity.PendingOperationEntity;
import com.cargohub.mobile.network.ApiClient;
import com.cargohub.mobile.network.ApiService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

/**
 * Manages a queue of offline mutations. When connectivity is restored,
 * replays them in FIFO order against the API.
 */
public final class SyncManager {

    private static final String TAG = "SyncManager";
    private static final int MAX_RETRIES = 5;
    private static final Gson GSON = new Gson();

    public static final String OP_CHANGE_STATE = "CHANGE_STATE";
    public static final String OP_CREATE_INCIDENCIA = "CREATE_INCIDENCIA";

    private static volatile SyncManager INSTANCE;

    private final PendingOperationDao pendingDao;
    private final ApiService apiService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private SyncManager(@NonNull Context context) {
        pendingDao = AppDatabase.getInstance(context).pendingOperationDao();
        apiService = ApiClient.getInstance();
    }

    public static SyncManager getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (SyncManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SyncManager(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Queue a porte state change for later sync.
     */
    public void queueStateChange(long porteId, @NonNull String nuevoEstado) {
        executor.execute(() -> {
            JsonObject payload = new JsonObject();
            payload.addProperty("porteId", porteId);
            payload.addProperty("nuevoEstado", nuevoEstado);

            PendingOperationEntity op = new PendingOperationEntity();
            op.operationType = OP_CHANGE_STATE;
            op.payload = payload.toString();
            op.createdAt = System.currentTimeMillis();
            op.retryCount = 0;
            pendingDao.insert(op);
            Log.d(TAG, "Queued state change: porte=" + porteId + " -> " + nuevoEstado);
        });
    }

    /**
     * Queue an incidencia creation for later sync.
     */
    public void queueCreateIncidencia(long porteId, @NonNull String titulo,
                                       @NonNull String descripcion,
                                       @NonNull String severidad,
                                       @NonNull String prioridad) {
        executor.execute(() -> {
            JsonObject payload = new JsonObject();
            payload.addProperty("porteId", porteId);
            payload.addProperty("titulo", titulo);
            payload.addProperty("descripcion", descripcion);
            payload.addProperty("severidad", severidad);
            payload.addProperty("prioridad", prioridad);

            PendingOperationEntity op = new PendingOperationEntity();
            op.operationType = OP_CREATE_INCIDENCIA;
            op.payload = payload.toString();
            op.createdAt = System.currentTimeMillis();
            op.retryCount = 0;
            pendingDao.insert(op);
            Log.d(TAG, "Queued incidencia: porte=" + porteId + " titulo=" + titulo);
        });
    }

    /**
     * Returns the count of pending operations (call from background thread).
     */
    public int getPendingCount() {
        return pendingDao.count();
    }

    /**
     * Attempt to sync all pending operations. Call when connectivity is restored.
     */
    public void syncAll() {
        executor.execute(this::doSync);
    }

    private void doSync() {
        List<PendingOperationEntity> pending = pendingDao.getAll();
        if (pending.isEmpty()) {
            Log.d(TAG, "No pending operations to sync.");
            return;
        }

        Log.d(TAG, "Syncing " + pending.size() + " pending operations...");

        for (PendingOperationEntity op : pending) {
            if (op.retryCount >= MAX_RETRIES) {
                Log.w(TAG, "Dropping operation " + op.id + " after " + MAX_RETRIES + " retries: " + op.lastError);
                pendingDao.deleteById(op.id);
                continue;
            }

            boolean success = false;
            try {
                success = executeOperation(op);
            } catch (Exception e) {
                Log.e(TAG, "Error executing operation " + op.id, e);
                pendingDao.markRetry(op.id, e.getMessage());
            }

            if (success) {
                pendingDao.deleteById(op.id);
                Log.d(TAG, "Synced operation " + op.id + " successfully.");
            }
        }
    }

    private boolean executeOperation(@NonNull PendingOperationEntity op) throws IOException {
        JsonObject payload = GSON.fromJson(op.payload, JsonObject.class);

        switch (op.operationType) {
            case OP_CHANGE_STATE: {
                long porteId = payload.get("porteId").getAsLong();
                String nuevoEstado = payload.get("nuevoEstado").getAsString();
                Response<?> response = apiService.changePorteState(porteId, nuevoEstado).execute();
                if (!response.isSuccessful()) {
                    pendingDao.markRetry(op.id, "HTTP " + response.code());
                    return false;
                }
                return true;
            }
            case OP_CREATE_INCIDENCIA: {
                long porteId = payload.get("porteId").getAsLong();
                com.cargohub.mobile.data.model.CrearIncidenciaRequest request =
                        new com.cargohub.mobile.data.model.CrearIncidenciaRequest(
                                payload.get("titulo").getAsString(),
                                payload.get("descripcion").getAsString(),
                                payload.get("severidad").getAsString(),
                                payload.get("prioridad").getAsString()
                        );
                Response<?> response = apiService.crearIncidencia(porteId, request).execute();
                if (!response.isSuccessful()) {
                    pendingDao.markRetry(op.id, "HTTP " + response.code());
                    return false;
                }
                return true;
            }
            default:
                Log.w(TAG, "Unknown operation type: " + op.operationType);
                return false;
        }
    }
}
