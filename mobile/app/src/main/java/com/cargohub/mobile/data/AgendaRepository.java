package com.cargohub.mobile.data;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.model.AgendaBloqueo;
import com.cargohub.mobile.data.model.AgendaBloqueoRequest;
import com.cargohub.mobile.data.model.BloqueoRecurrente;
import com.cargohub.mobile.network.ApiClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgendaRepository {

    private final com.cargohub.mobile.network.ApiService apiService;

    public AgendaRepository() {
        this(ApiClient.getInstance());
    }

    AgendaRepository(@NonNull com.cargohub.mobile.network.ApiService apiService) {
        this.apiService = apiService;
    }

    public void getAgenda(long conductorId,
                          @NonNull String desde,
                          @NonNull String hasta,
                          @NonNull RepositoryCallback<List<AgendaBloqueo>> callback) {
        apiService
                .getAgenda(conductorId, desde, hasta)
                .enqueue(new Callback<List<AgendaBloqueo>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<AgendaBloqueo>> call,
                                           @NonNull Response<List<AgendaBloqueo>> response) {
                        callback.onResult(RepositorySupport.fromResponse(response, "No se pudo cargar tu agenda."));
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<AgendaBloqueo>> call, @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al cargar la agenda.",
                                "Error de red al cargar la agenda."
                        ));
                    }
                });
    }

    public void createBloqueo(long conductorId,
                              @NonNull AgendaBloqueoRequest request,
                              @NonNull RepositoryCallback<AgendaBloqueo> callback) {
        apiService
                .createAgendaBloqueo(conductorId, request)
                .enqueue(new Callback<AgendaBloqueo>() {
                    @Override
                    public void onResponse(@NonNull Call<AgendaBloqueo> call,
                                           @NonNull Response<AgendaBloqueo> response) {
                        callback.onResult(RepositorySupport.fromResponse(response, "No se pudo guardar el bloqueo de agenda."));
                    }

                    @Override
                    public void onFailure(@NonNull Call<AgendaBloqueo> call, @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al guardar la agenda.",
                                "Error de red al guardar la agenda."
                        ));
                    }
                });
    }

    public void deleteBloqueo(long bloqueoId, @NonNull RepositoryCallback<Void> callback) {
        apiService
                .deleteAgendaBloqueo(bloqueoId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        callback.onResult(RepositorySupport.fromResponse(response, "No se pudo eliminar el bloqueo de agenda."));
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al eliminar el bloqueo.",
                                "Error de red al eliminar el bloqueo."
                        ));
                    }
                });
    }

    public void getBloqueoRecurrentes(long conductorId,
                                       @NonNull RepositoryCallback<List<BloqueoRecurrente>> callback) {
        apiService
                .getBloqueoRecurrentes(conductorId)
                .enqueue(new Callback<List<BloqueoRecurrente>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<BloqueoRecurrente>> call,
                                           @NonNull Response<List<BloqueoRecurrente>> response) {
                        callback.onResult(RepositorySupport.fromResponse(response, "No se pudieron cargar los bloqueos recurrentes."));
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<BloqueoRecurrente>> call, @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al cargar bloqueos recurrentes.",
                                "Error de red al cargar bloqueos recurrentes."
                        ));
                    }
                });
    }

    public void setBloqueoRecurrentes(long conductorId,
                                       @NonNull List<Integer> diasBloqueados,
                                       @NonNull RepositoryCallback<List<BloqueoRecurrente>> callback) {
        apiService
                .setBloqueoRecurrentes(conductorId, diasBloqueados)
                .enqueue(new Callback<List<BloqueoRecurrente>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<BloqueoRecurrente>> call,
                                           @NonNull Response<List<BloqueoRecurrente>> response) {
                        callback.onResult(RepositorySupport.fromResponse(response, "No se pudieron guardar los bloqueos recurrentes."));
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<BloqueoRecurrente>> call, @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al guardar bloqueos recurrentes.",
                                "Error de red al guardar bloqueos recurrentes."
                        ));
                    }
                });
    }
}
