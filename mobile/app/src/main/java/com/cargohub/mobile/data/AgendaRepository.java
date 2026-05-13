package com.cargohub.mobile.data;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.model.AgendaBloqueo;
import com.cargohub.mobile.data.model.AgendaBloqueoRequest;
import com.cargohub.mobile.data.model.BloqueoRecurrente;
import com.cargohub.mobile.network.ApiClient;

import java.util.ArrayList;
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

    public void getDiasLaborables(long conductorId,
                                  @NonNull RepositoryCallback<List<Integer>> callback) {
        apiService
                .getDiasLaborables(conductorId)
                .enqueue(new Callback<List<Integer>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Integer>> call,
                                           @NonNull Response<List<Integer>> response) {
                        if (response.isSuccessful()) {
                            callback.onResult(RepositorySupport.fromResponse(response, "No se pudieron cargar los días laborables."));
                            return;
                        }
                        if (isLegacyRecurringFallbackCandidate(response.code())) {
                            getDiasLaborablesLegacy(conductorId, callback);
                            return;
                        }
                        callback.onResult(RepositorySupport.fromResponse(response, "No se pudieron cargar los días laborables."));
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Integer>> call, @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al cargar días laborables.",
                                "Error de red al cargar días laborables."
                        ));
                    }
                });
    }

    public void setDiasLaborables(long conductorId,
                                  @NonNull List<Integer> diasLaborables,
                                  @NonNull RepositoryCallback<List<Integer>> callback) {
        List<Integer> normalizedWorkingDays = normalizeWorkingDays(diasLaborables);
        apiService
                .setDiasLaborables(conductorId, normalizedWorkingDays)
                .enqueue(new Callback<List<Integer>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Integer>> call,
                                           @NonNull Response<List<Integer>> response) {
                        if (response.isSuccessful()) {
                            callback.onResult(RepositorySupport.fromResponse(response, "No se pudieron guardar los días laborables."));
                            return;
                        }
                        if (isLegacyRecurringFallbackCandidate(response.code())) {
                            setDiasLaborablesLegacy(conductorId, normalizedWorkingDays, callback);
                            return;
                        }
                        callback.onResult(RepositorySupport.fromResponse(response, "No se pudieron guardar los días laborables."));
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Integer>> call, @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al guardar días laborables.",
                                "Error de red al guardar días laborables."
                        ));
                    }
                });
    }

    private void getDiasLaborablesLegacy(long conductorId,
                                         @NonNull RepositoryCallback<List<Integer>> callback) {
        apiService
                .getBloqueoRecurrentes(conductorId)
                .enqueue(new Callback<List<BloqueoRecurrente>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<BloqueoRecurrente>> call,
                                           @NonNull Response<List<BloqueoRecurrente>> response) {
                        if (!response.isSuccessful()) {
                            callback.onResult(RepositoryResult.error(
                                    "No se pudieron cargar los días laborables.",
                                    response.code(),
                                    RepositorySupport.isUnauthorized(response.code())
                            ));
                            return;
                        }
                        List<Integer> workingDays = toWorkingDays(response.body());
                        callback.onResult(RepositoryResult.success(workingDays));
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<BloqueoRecurrente>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al cargar días laborables.",
                                "Error de red al cargar días laborables."
                        ));
                    }
                });
    }

    private void setDiasLaborablesLegacy(long conductorId,
                                         @NonNull List<Integer> workingDays,
                                         @NonNull RepositoryCallback<List<Integer>> callback) {
        apiService
                .setBloqueoRecurrentes(conductorId, toBlockedDays(workingDays))
                .enqueue(new Callback<List<BloqueoRecurrente>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<BloqueoRecurrente>> call,
                                           @NonNull Response<List<BloqueoRecurrente>> response) {
                        if (!response.isSuccessful()) {
                            callback.onResult(RepositoryResult.error(
                                    "No se pudieron guardar los días laborables.",
                                    response.code(),
                                    RepositorySupport.isUnauthorized(response.code())
                            ));
                            return;
                        }
                        List<Integer> persistedWorkingDays = toWorkingDays(response.body());
                        callback.onResult(RepositoryResult.success(persistedWorkingDays));
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<BloqueoRecurrente>> call,
                                          @NonNull Throwable t) {
                        callback.onResult(RepositorySupport.fromFailure(
                                t,
                                "Tiempo de espera agotado al guardar días laborables.",
                                "Error de red al guardar días laborables."
                        ));
                    }
                });
    }

    private boolean isLegacyRecurringFallbackCandidate(int code) {
        return code == 404 || code == 405;
    }

    @NonNull
    private List<Integer> normalizeWorkingDays(@NonNull List<Integer> days) {
        List<Integer> normalized = new ArrayList<>();
        for (Integer day : days) {
            if (day != null && day >= 1 && day <= 7 && !normalized.contains(day)) {
                normalized.add(day);
            }
        }
        normalized.sort(Integer::compareTo);
        return normalized;
    }

    @NonNull
    private List<Integer> toBlockedDays(@NonNull List<Integer> workingDays) {
        List<Integer> blocked = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            if (!workingDays.contains(day)) {
                blocked.add(day);
            }
        }
        return blocked;
    }

    @NonNull
    private List<Integer> toWorkingDays(List<BloqueoRecurrente> bloqueos) {
        List<Integer> blocked = new ArrayList<>();
        if (bloqueos != null) {
            for (BloqueoRecurrente bloqueo : bloqueos) {
                if (bloqueo == null) {
                    continue;
                }
                int day = bloqueo.getDiaSemana();
                if (day < 1 || day > 7) {
                    continue;
                }
                if (bloqueo.isActivo() && !blocked.contains(day)) {
                    blocked.add(day);
                }
            }
        }

        List<Integer> working = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            if (!blocked.contains(day)) {
                working.add(day);
            }
        }
        return working;
    }
}
