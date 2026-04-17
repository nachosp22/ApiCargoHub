package com.cargohub.mobile.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.network.ApiClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PorteRepository {

    private final com.cargohub.mobile.network.ApiService apiService;

    public PorteRepository() {
        this(ApiClient.getInstance());
    }

    PorteRepository(@NonNull com.cargohub.mobile.network.ApiService apiService) {
        this.apiService = apiService;
    }

    public enum PorteAction {
        ACCEPT_OFFER,
        REJECT_OFFER,
        START_TRIP,
        COMPLETE_TRIP
    }

    public void getOffers(long conductorId, @NonNull RepositoryCallback<List<Porte>> callback) {
        apiService.getPortesOferta(conductorId).enqueue(new Callback<List<Porte>>() {
            @Override
            public void onResponse(@NonNull Call<List<Porte>> call, @NonNull Response<List<Porte>> response) {
                callback.onResult(RepositorySupport.fromResponse(response, "No se pudieron cargar las ofertas."));
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
        apiService.getPortesDelConductor(conductorId).enqueue(new Callback<List<Porte>>() {
            @Override
            public void onResponse(@NonNull Call<List<Porte>> call, @NonNull Response<List<Porte>> response) {
                callback.onResult(RepositorySupport.fromResponse(response, "No se pudieron cargar los portes."));
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
        apiService.getPorteDetail(porteId).enqueue(new Callback<Porte>() {
            @Override
            public void onResponse(@NonNull Call<Porte> call, @NonNull Response<Porte> response) {
                callback.onResult(RepositorySupport.fromResponse(response, "No se pudo cargar el detalle del porte."));
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
