package com.cargohub.mobile.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.data.local.AppDatabase;
import com.cargohub.mobile.data.local.EntityMapper;
import com.cargohub.mobile.data.local.OfflineSupport;
import com.cargohub.mobile.data.local.SyncManager;
import com.cargohub.mobile.data.local.dao.PorteDao;
import com.cargohub.mobile.data.local.entity.PorteEntity;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.network.ApiClient;
import com.cargohub.mobile.network.ConnectivityObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PorteRepository {

    private final com.cargohub.mobile.network.ApiService apiService;
    @Nullable private final PorteDao porteDao;
    @Nullable private final Context context;

    public PorteRepository() {
        this(ApiClient.getInstance(), null, null);
    }

    public PorteRepository(@NonNull Context context) {
        this(ApiClient.getInstance(),
             AppDatabase.getInstance(context).porteDao(),
             context);
    }

    PorteRepository(@NonNull com.cargohub.mobile.network.ApiService apiService,
                    @Nullable PorteDao porteDao,
                    @Nullable Context context) {
        this.apiService = apiService;
        this.porteDao = porteDao;
        this.context = context;
    }

    public enum PorteAction {
        ACCEPT_OFFER,
        REJECT_OFFER,
        START_TRIP,
        COMPLETE_TRIP
    }

    public void getOffers(long conductorId, @NonNull RepositoryCallback<List<Porte>> callback) {
        // Serve from cache if offline
        if (context != null && porteDao != null) {
            boolean served = OfflineSupport.serveCacheIfOffline(context,
                    () -> {
                        List<PorteEntity> cached = porteDao.getOffers(conductorId);
                        return cached.isEmpty() ? null : EntityMapper.toPortes(cached);
                    }, callback);
            if (served) return;
        }

        apiService.getPortesOferta(conductorId).enqueue(new Callback<List<Porte>>() {
            @Override
            public void onResponse(@NonNull Call<List<Porte>> call, @NonNull Response<List<Porte>> response) {
                RepositoryResult<List<Porte>> result = RepositorySupport.fromResponse(response, "No se pudieron cargar las ofertas.");
                if (result.isSuccessful() && result.getData() != null && porteDao != null) {
                    OfflineSupport.cacheInBackground(() -> {
                        porteDao.deleteByConductorAndType(conductorId, true);
                        porteDao.insertAll(EntityMapper.toEntities(result.getData(), conductorId, true));
                    });
                }
                callback.onResult(result);
            }

            @Override
            public void onFailure(@NonNull Call<List<Porte>> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al cargar las ofertas.",
                        "Error de red al cargar las ofertas."
                ));
            }
        });
    }

    public void getAssignedTrips(long conductorId, @NonNull RepositoryCallback<List<Porte>> callback) {
        // Serve from cache if offline
        if (context != null && porteDao != null) {
            boolean served = OfflineSupport.serveCacheIfOffline(context,
                    () -> {
                        List<PorteEntity> cached = porteDao.getAssignedTrips(conductorId);
                        return cached.isEmpty() ? null : EntityMapper.toPortes(cached);
                    }, callback);
            if (served) return;
        }

        apiService.getPortesDelConductor(conductorId).enqueue(new Callback<List<Porte>>() {
            @Override
            public void onResponse(@NonNull Call<List<Porte>> call, @NonNull Response<List<Porte>> response) {
                RepositoryResult<List<Porte>> result = RepositorySupport.fromResponse(response, "No se pudieron cargar los portes.");
                if (result.isSuccessful() && result.getData() != null && porteDao != null) {
                    OfflineSupport.cacheInBackground(() -> {
                        porteDao.deleteByConductorAndType(conductorId, false);
                        porteDao.insertAll(EntityMapper.toEntities(result.getData(), conductorId, false));
                    });
                }
                callback.onResult(result);
            }

            @Override
            public void onFailure(@NonNull Call<List<Porte>> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al cargar los portes.",
                        "Error de red al cargar los portes."
                ));
            }
        });
    }

    public void getPorteDetail(long porteId, @NonNull RepositoryCallback<Porte> callback) {
        // Serve from cache if offline
        if (context != null && porteDao != null) {
            boolean served = OfflineSupport.serveCacheIfOffline(context,
                    () -> {
                        PorteEntity cached = porteDao.getById(porteId);
                        return cached != null ? EntityMapper.toPorte(cached) : null;
                    }, callback);
            if (served) return;
        }

        apiService.getPorteDetail(porteId).enqueue(new Callback<Porte>() {
            @Override
            public void onResponse(@NonNull Call<Porte> call, @NonNull Response<Porte> response) {
                RepositoryResult<Porte> result = RepositorySupport.fromResponse(response, "No se pudo cargar el detalle del porte.");
                if (result.isSuccessful() && result.getData() != null && porteDao != null) {
                    OfflineSupport.cacheInBackground(() ->
                            porteDao.insert(EntityMapper.toEntity(result.getData(), 0, false)));
                }
                callback.onResult(result);
            }

            @Override
            public void onFailure(@NonNull Call<Porte> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al cargar el detalle del porte.",
                        "Error de red al cargar el detalle del porte."
                ));
            }
        });
    }

    public void acceptOffer(long porteId, long conductorId, @NonNull RepositoryCallback<Porte> callback) {
        apiService.acceptPorteOffer(porteId, conductorId).enqueue(new Callback<Porte>() {
            @Override
            public void onResponse(@NonNull Call<Porte> call, @NonNull Response<Porte> response) {
                if (!response.isSuccessful()) {
                    callback.onResult(RepositorySupport.fromResponse(response, "No se pudo aceptar la oferta."));
                    return;
                }
                refreshPorteDetail(porteId, callback, "No se pudo refrescar el porte despues de aceptar la oferta.");
            }

            @Override
            public void onFailure(@NonNull Call<Porte> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al aceptar la oferta.",
                        "Error de red al aceptar la oferta."
                ));
            }
        });
    }

    public void rejectOffer(long porteId, long conductorId, @NonNull RepositoryCallback<Void> callback) {
        apiService.rejectPorteOffer(porteId, conductorId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                callback.onResult(RepositorySupport.fromResponse(response, "No se pudo rechazar la oferta."));
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al rechazar la oferta.",
                        "Error de red al rechazar la oferta."
                ));
            }
        });
    }

    public void changeTripState(long porteId,
                                @NonNull EstadoPorte currentState,
                                @NonNull EstadoPorte nextState,
                                @NonNull RepositoryCallback<Porte> callback) {
        if (!isTransitionAllowed(currentState, nextState)) {
            callback.onResult(RepositoryResult.error("La accion seleccionada no es valida para el estado actual.", 400, false));
            return;
        }

        // Queue offline if no connection
        if (context != null && !ConnectivityObserver.getInstance(context).isOnline()) {
            SyncManager.getInstance(context).queueStateChange(porteId, nextState.name());
            // Optimistically update cache
            if (porteDao != null) {
                OfflineSupport.cacheInBackground(() -> {
                    PorteEntity cached = porteDao.getById(porteId);
                    if (cached != null) {
                        cached.estado = nextState.name();
                        porteDao.insert(cached);
                    }
                });
            }
            // Return a synthetic cached result
            Porte optimistic = new Porte();
            optimistic.setId(porteId);
            optimistic.setEstado(nextState.name());
            callback.onResult(RepositoryResult.cached(optimistic));
            return;
        }

        apiService.changePorteState(porteId, nextState.name()).enqueue(new Callback<Porte>() {
            @Override
            public void onResponse(@NonNull Call<Porte> call, @NonNull Response<Porte> response) {
                if (!response.isSuccessful()) {
                    callback.onResult(RepositorySupport.fromResponse(response, "No se pudo actualizar el estado del porte."));
                    return;
                }
                refreshPorteDetail(porteId, callback, "No se pudo refrescar el porte despues del cambio de estado.");
            }

            @Override
            public void onFailure(@NonNull Call<Porte> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al actualizar el estado.",
                        "Error de red al actualizar el estado del porte."
                ));
            }
        });
    }

    public boolean canAcceptOffer(@Nullable Porte porte) {
        return porte != null && porte.getEstadoPorte() == EstadoPorte.PENDIENTE;
    }

    public boolean supportsOfferRejection() {
        return true;
    }

    @NonNull
    public List<PorteAction> getAvailableActions(@Nullable Porte porte) {
        if (porte == null || porte.getEstadoPorte() == null) {
            return Collections.emptyList();
        }

        List<PorteAction> actions = new ArrayList<>();
        switch (porte.getEstadoPorte()) {
            case PENDIENTE:
                actions.add(PorteAction.ACCEPT_OFFER);
                actions.add(PorteAction.REJECT_OFFER);
                break;
            case ASIGNADO:
                actions.add(PorteAction.START_TRIP);
                break;
            case EN_TRANSITO:
                actions.add(PorteAction.COMPLETE_TRIP);
                break;
            default:
                break;
        }
        return actions;
    }

    public boolean isTransitionAllowed(@NonNull EstadoPorte currentState, @NonNull EstadoPorte nextState) {
        if (currentState == EstadoPorte.ASIGNADO && nextState == EstadoPorte.EN_TRANSITO) {
            return true;
        }
        return currentState == EstadoPorte.EN_TRANSITO && nextState == EstadoPorte.ENTREGADO;
    }

    public void firmarEntrega(long porteId, @NonNull String firmaBase64, @NonNull String firmadoPor,
                              @NonNull RepositoryCallback<Porte> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("firmaBase64", firmaBase64);
        body.put("firmadoPor", firmadoPor);

        apiService.firmarEntrega(porteId, body).enqueue(new Callback<Porte>() {
            @Override
            public void onResponse(@NonNull Call<Porte> call, @NonNull Response<Porte> response) {
                callback.onResult(RepositorySupport.fromResponse(response, "No se pudo registrar la firma de entrega."));
            }

            @Override
            public void onFailure(@NonNull Call<Porte> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al registrar la firma.",
                        "Error de red al registrar la firma de entrega."
                ));
            }
        });
    }

    private void refreshPorteDetail(long porteId,
                                    @NonNull RepositoryCallback<Porte> callback,
                                    @NonNull String fallbackMessage) {
        apiService.getPorteDetail(porteId).enqueue(new Callback<Porte>() {
            @Override
            public void onResponse(@NonNull Call<Porte> call, @NonNull Response<Porte> response) {
                callback.onResult(RepositorySupport.fromResponse(response, fallbackMessage));
            }

            @Override
            public void onFailure(@NonNull Call<Porte> call, @NonNull Throwable t) {
                callback.onResult(RepositorySupport.fromFailure(
                        t,
                        "Tiempo de espera agotado al refrescar el porte.",
                        "Error de red al refrescar el porte."
                ));
            }
        });
    }
}
