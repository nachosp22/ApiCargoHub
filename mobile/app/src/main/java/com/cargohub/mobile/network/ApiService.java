package com.cargohub.mobile.network;

import com.cargohub.mobile.data.model.LoginResponse;
import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.data.model.IncidenciaResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @FormUrlEncoded
    @POST("api/auth/login")
    Call<LoginResponse> login(@Field("email") String email, @Field("password") String password);

    @GET("api/conductores/{id}")
    Call<ConductorProfileResponse> getConductorProfile(@Path("id") long conductorId);

    @GET("api/portes/conductor/{conductorId}")
    Call<List<Porte>> getPortesDelConductor(@Path("conductorId") long conductorId);

    @GET("api/incidencias/porte/{porteId}")
    Call<List<IncidenciaResponse>> getIncidenciasPorPorte(@Path("porteId") long porteId);

    @FormUrlEncoded
    @POST("api/incidencias")
    Call<IncidenciaResponse> crearIncidencia(
            @Query("porteId") long porteId,
            @Field("titulo") String titulo,
            @Field("descripcion") String descripcion,
            @Field("severidad") String severidad,
            @Field("prioridad") String prioridad
    );
}
