package com.cargohub.mobile.network;

import com.cargohub.mobile.data.model.EstadisticasConductor;
import com.cargohub.mobile.data.model.FacturaPageResponse;
import com.cargohub.mobile.data.model.FacturaResumen;
import com.cargohub.mobile.data.model.LoginResponse;
import com.cargohub.mobile.data.model.AgendaBloqueo;
import com.cargohub.mobile.data.model.AgendaBloqueoRequest;
import com.cargohub.mobile.data.model.BloqueoRecurrente;
import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.data.model.ConductorProfileUpdateRequest;
import com.cargohub.mobile.data.model.CrearIncidenciaRequest;
import com.cargohub.mobile.data.model.DriverLocationUpdateRequest;
import com.cargohub.mobile.data.model.IncidenciaEventoResponse;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.data.model.IncidenciaResponse;
import com.cargohub.mobile.data.model.Vehiculo;
import com.cargohub.mobile.data.model.VehiculoUpsertRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @FormUrlEncoded
    @POST("api/auth/login")
    Call<LoginResponse> login(@Field("email") String email, @Field("password") String password);

    @GET("api/conductores/{id}")
    Call<ConductorProfileResponse> getConductorProfile(@Path("id") long conductorId);

    @PUT("api/conductores/{id}")
    Call<ConductorProfileResponse> updateConductorProfile(@Path("id") long conductorId,
                                                          @Body ConductorProfileUpdateRequest request);

    @DELETE("api/conductores/{id}")
    Call<Void> deactivateConductorProfile(@Path("id") long conductorId);

    @GET("api/conductores/{id}/agenda")
    Call<List<AgendaBloqueo>> getAgenda(@Path("id") long conductorId,
                                        @Query("desde") String desde,
                                        @Query("hasta") String hasta);

    @POST("api/conductores/{id}/agenda")
    Call<AgendaBloqueo> createAgendaBloqueo(@Path("id") long conductorId,
                                            @Body AgendaBloqueoRequest request);

    @DELETE("api/conductores/agenda/{bloqueoId}")
    Call<Void> deleteAgendaBloqueo(@Path("bloqueoId") long bloqueoId);

    @GET("api/conductores/{id}/bloqueos-recurrentes")
    Call<List<BloqueoRecurrente>> getBloqueoRecurrentes(@Path("id") long conductorId);

    @PUT("api/conductores/{id}/bloqueos-recurrentes")
    Call<List<BloqueoRecurrente>> setBloqueoRecurrentes(@Path("id") long conductorId,
                                                        @Body List<Integer> diasBloqueados);

    @GET("api/conductores/{conductorId}/vehiculos")
    Call<List<Vehiculo>> getVehiculos(@Path("conductorId") long conductorId);

    @POST("api/conductores/{conductorId}/vehiculos")
    Call<Vehiculo> createVehiculo(@Path("conductorId") long conductorId,
                                  @Body VehiculoUpsertRequest request);

    @PUT("api/conductores/{conductorId}/vehiculos/{vehiculoId}/activar")
    Call<Void> activateVehiculo(@Path("conductorId") long conductorId,
                                @Path("vehiculoId") long vehiculoId);

    @PUT("api/conductores/{conductorId}/vehiculos/{vehiculoId}/desactivar")
    Call<Void> deactivateVehiculo(@Path("conductorId") long conductorId,
                                  @Path("vehiculoId") long vehiculoId);

    @GET("api/portes/ofertas/{conductorId}")
    Call<List<Porte>> getPortesOferta(@Path("conductorId") long conductorId);

    @GET("api/portes/conductor/{conductorId}")
    Call<List<Porte>> getPortesDelConductor(@Path("conductorId") long conductorId);

    @GET("api/portes/{porteId}")
    Call<Porte> getPorteDetail(@Path("porteId") long porteId);

    @POST("api/portes/{porteId}/aceptar")
    Call<Porte> acceptPorteOffer(@Path("porteId") long porteId,
                                 @Query("conductorId") long conductorId);

    @POST("api/portes/{porteId}/rechazar")
    Call<Void> rejectPorteOffer(@Path("porteId") long porteId,
                                @Query("conductorId") long conductorId);

    @PUT("api/portes/{porteId}/estado")
    Call<Porte> changePorteState(@Path("porteId") long porteId,
                                 @Query("nuevo") String nuevoEstado);

    @GET("api/incidencias/porte/{porteId}")
    Call<List<IncidenciaResponse>> getIncidenciasPorPorte(@Path("porteId") long porteId);

    @GET("api/incidencias/{id}")
    Call<IncidenciaResponse> getIncidenciaById(@Path("id") long incidenciaId);

    @GET("api/incidencias/{id}/historial")
    Call<List<IncidenciaEventoResponse>> getIncidenciaHistorial(@Path("id") long incidenciaId);

    @POST("api/incidencias")
    Call<IncidenciaResponse> crearIncidencia(
            @Query("porteId") long porteId,
            @Body CrearIncidenciaRequest request
    );

    @POST("api/v1/tracking/drivers/{driverId}/locations")
    Call<Void> upsertDriverLocation(@Path("driverId") long driverId,
                                    @Body DriverLocationUpdateRequest request);

    @POST("api/conductores/{id}/ubicacion")
    Call<Void> reportLegacyConductorLocation(@Path("id") long conductorId,
                                              @Query("lat") double lat,
                                              @Query("lon") double lon);

    // ── Facturación conductor ──

    @GET("api/conductores/{id}/facturas")
    Call<FacturaPageResponse> getFacturas(@Path("id") long conductorId,
                                          @Query("desde") String desde,
                                          @Query("hasta") String hasta,
                                          @Query("pagada") Boolean pagada,
                                          @Query("page") int page,
                                          @Query("size") int size);

    @GET("api/conductores/{id}/facturas/resumen")
    Call<FacturaResumen> getFacturasResumen(@Path("id") long conductorId,
                                            @Query("periodo") String periodo);

    // ── Estadísticas conductor ──

    @GET("api/conductores/{id}/estadisticas")
    Call<EstadisticasConductor> getEstadisticas(@Path("id") long conductorId,
                                                @Query("desde") String desde,
                                                @Query("hasta") String hasta);
}
