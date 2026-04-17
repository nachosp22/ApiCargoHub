package com.cargohub.mobile.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import com.cargohub.mobile.data.model.AgendaBloqueo;
import com.cargohub.mobile.data.model.ConductorProfileResponse;
import com.cargohub.mobile.data.model.ConductorProfileUpdateRequest;
import com.cargohub.mobile.data.model.DriverLocationUpdateRequest;
import com.cargohub.mobile.data.model.EstadoPorte;
import com.cargohub.mobile.data.model.IncidenciaEventoResponse;
import com.cargohub.mobile.data.model.IncidenciaResponse;
import com.cargohub.mobile.data.model.LoginResponse;
import com.cargohub.mobile.data.model.Porte;
import com.cargohub.mobile.data.model.TipoVehiculo;
import com.cargohub.mobile.data.model.Vehiculo;
import com.cargohub.mobile.data.model.VehiculoUpsertRequest;
import com.cargohub.mobile.session.SessionSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RepositoryIntegrationTest {

    private MockWebServer server;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void login_postsFormDataAndParsesSessionFields() throws Exception {
        server.enqueue(jsonResponse(200, "{\"accessToken\":\"jwt-token\",\"expiresAt\":\"2030-01-01T10:15:30Z\",\"rol\":\"CONDUCTOR\",\"conductorId\":7}"));

        AuthRepository repository = new AuthRepository(createApiService());
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<LoginResponse> responseRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        repository.login("driver@cargo.test", "secret123", new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess(LoginResponse loginResponse) {
                responseRef.set(loginResponse);
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                errorRef.set(message);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(errorRef.get());
        assertNotNull(responseRef.get());
        assertEquals("jwt-token", responseRef.get().getToken());

        SessionSnapshot snapshot = SessionSnapshot.fromLoginResponse(responseRef.get());
        assertEquals(Long.valueOf(7L), snapshot.getConductorId());
        assertTrue(snapshot.isActive(System.currentTimeMillis()));

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/api/auth/login", request.getPath());
        assertEquals("application/x-www-form-urlencoded", request.getHeader("Content-Type"));
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("email=driver%40cargo.test"));
        assertTrue(body.contains("password=secret123"));
    }

    @Test
    public void getAgenda_requestsRangeAndParsesItems() throws Exception {
        server.enqueue(jsonResponse(200, "[{\"id\":15,\"fechaInicio\":\"2026-03-28T08:00:00\",\"fechaFin\":\"2026-03-28T10:00:00\",\"tipo\":\"DESCANSO\",\"titulo\":\"Descanso\"}]"));

        AgendaRepository repository = new AgendaRepository(createApiService());
        CallbackCapture<List<AgendaBloqueo>> capture = new CallbackCapture<>();

        repository.getAgenda(5L, "2026-03-28", "2026-03-29", capture);

        RepositoryResult<List<AgendaBloqueo>> result = capture.await();
        assertTrue(result.isSuccessful());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/api/conductores/5/agenda?desde=2026-03-28&hasta=2026-03-29", request.getPath());
    }

    @Test
    public void updateProfile_putsEditableDriverFields() throws Exception {
        server.enqueue(jsonResponse(200, "{\"id\":5,\"nombre\":\"Ana\",\"apellidos\":\"Lopez\",\"telefono\":\"600111222\",\"ciudadBase\":\"Madrid\"}"));

        ConductorRepository repository = new ConductorRepository(createApiService());
        CallbackCapture<ConductorProfileResponse> capture = new CallbackCapture<>();

        repository.updateConductorProfile(5L, new ConductorProfileUpdateRequest(
                "Ana",
                "Lopez",
                "600111222",
                "Madrid"
        ), capture);

        RepositoryResult<ConductorProfileResponse> result = capture.await();
        assertTrue(result.isSuccessful());
        assertNotNull(result.getData());

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("PUT", request.getMethod());
        assertEquals("/api/conductores/5", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"nombre\":\"Ana\""));
        assertTrue(body.contains("\"apellidos\":\"Lopez\""));
        assertTrue(body.contains("\"telefono\":\"600111222\""));
        assertTrue(body.contains("\"ciudadBase\":\"Madrid\""));
    }

    @Test
    public void createVehiculo_postsJsonPayload() throws Exception {
        server.enqueue(jsonResponse(200, "{\"id\":22,\"matricula\":\"1234ABC\",\"marca\":\"Volvo\",\"modelo\":\"FM\",\"tipo\":\"TRAILER\"}"));

        VehiculoRepository repository = new VehiculoRepository(createApiService());
        CallbackCapture<Vehiculo> capture = new CallbackCapture<>();

        repository.createVehiculo(9L, new VehiculoUpsertRequest(
                "1234ABC",
                "Volvo",
                "FM",
                TipoVehiculo.TRAILER,
                12000,
                6000,
                2400,
                2600,
                true
        ), capture);

        RepositoryResult<Vehiculo> result = capture.await();
        assertTrue(result.isSuccessful());
        assertNotNull(result.getData());
        assertEquals("1234ABC", result.getData().getMatricula());

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/api/conductores/9/vehiculos", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"matricula\":\"1234ABC\""));
        assertTrue(body.contains("\"tipo\":\"TRAILER\""));
        assertTrue(body.contains("\"trampillaElevadora\":true"));
    }

    @Test
    public void crearIncidencia_postsJsonPayloadWithPorteContext() throws Exception {
        server.enqueue(jsonResponse(200, "{\"id\":91,\"titulo\":\"Golpe lateral\",\"descripcion\":\"Danio detectado\",\"estado\":\"ABIERTA\"}"));

        IncidenciaRepository repository = new IncidenciaRepository(createApiService());
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<IncidenciaResponse> responseRef = new AtomicReference<>();
        AtomicReference<String> errorRef = new AtomicReference<>();

        repository.crearIncidencia(18L, "Golpe lateral", "Danio detectado", "ALTA", "ALTA", new IncidenciaRepository.CrearCallback() {
            @Override
            public void onSuccess(IncidenciaResponse incidencia) {
                responseRef.set(incidencia);
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                errorRef.set(message);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(errorRef.get());
        assertNotNull(responseRef.get());

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/api/incidencias?porteId=18", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"titulo\":\"Golpe lateral\""));
        assertTrue(body.contains("\"descripcion\":\"Danio detectado\""));
        assertTrue(body.contains("\"prioridad\":\"ALTA\""));
    }

    @Test
    public void incidenciaDetailAndHistorial_useDedicatedEndpoints() throws Exception {
        server.enqueue(jsonResponse(200, "{\"id\":91,\"titulo\":\"Golpe lateral\",\"descripcion\":\"Danio detectado\",\"estado\":\"ABIERTA\"}"));
        server.enqueue(jsonResponse(200, "[{\"id\":1,\"incidenciaId\":91,\"accion\":\"CREADA\",\"estadoNuevo\":\"ABIERTA\",\"fecha\":\"2026-03-27T20:30:00\"}]"));

        IncidenciaRepository repository = new IncidenciaRepository(createApiService());

        CountDownLatch detailLatch = new CountDownLatch(1);
        AtomicReference<IncidenciaResponse> detailRef = new AtomicReference<>();
        AtomicReference<String> detailErrorRef = new AtomicReference<>();

        repository.getPorId(91L, new IncidenciaRepository.IncidenciaDetalleCallback() {
            @Override
            public void onSuccess(@NonNull IncidenciaResponse incidencia) {
                detailRef.set(incidencia);
                detailLatch.countDown();
            }

            @Override
            public void onError(@NonNull String message) {
                detailErrorRef.set(message);
                detailLatch.countDown();
            }
        });

        assertTrue(detailLatch.await(5, TimeUnit.SECONDS));
        assertNull(detailErrorRef.get());
        assertNotNull(detailRef.get());

        CountDownLatch historyLatch = new CountDownLatch(1);
        AtomicReference<List<IncidenciaEventoResponse>> historyRef = new AtomicReference<>();
        AtomicReference<String> historyErrorRef = new AtomicReference<>();

        repository.getHistorialIncidencia(91L, new IncidenciaRepository.HistorialCallback() {
            @Override
            public void onSuccess(@NonNull List<IncidenciaEventoResponse> historial) {
                historyRef.set(historial);
                historyLatch.countDown();
            }

            @Override
            public void onError(@NonNull String message) {
                historyErrorRef.set(message);
                historyLatch.countDown();
            }
        });

        assertTrue(historyLatch.await(5, TimeUnit.SECONDS));
        assertNull(historyErrorRef.get());
        assertNotNull(historyRef.get());
        assertEquals(1, historyRef.get().size());

        RecordedRequest detailRequest = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest historyRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(detailRequest);
        assertNotNull(historyRequest);
        assertEquals("GET", detailRequest.getMethod());
        assertEquals("/api/incidencias/91", detailRequest.getPath());
        assertEquals("GET", historyRequest.getMethod());
        assertEquals("/api/incidencias/91/historial", historyRequest.getPath());
    }

    @Test
    public void acceptOffer_refreshesDetailAfterMutation() throws Exception {
        server.enqueue(jsonResponse(200, "{\"id\":55,\"estado\":\"ASIGNADO\"}"));
        server.enqueue(jsonResponse(200, "{\"id\":55,\"origen\":\"Madrid\",\"destino\":\"Valencia\",\"estado\":\"ASIGNADO\"}"));

        PorteRepository repository = new PorteRepository(createApiService());
        CallbackCapture<Porte> capture = new CallbackCapture<>();

        repository.acceptOffer(55L, 9L, capture);

        RepositoryResult<Porte> result = capture.await();
        assertTrue(result.isSuccessful());
        assertNotNull(result.getData());
        assertEquals(EstadoPorte.ASIGNADO, result.getData().getEstadoPorte());

        RecordedRequest acceptRequest = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest detailRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(acceptRequest);
        assertNotNull(detailRequest);
        assertEquals("POST", acceptRequest.getMethod());
        assertEquals("/api/portes/55/aceptar?conductorId=9", acceptRequest.getPath());
        assertEquals("GET", detailRequest.getMethod());
        assertEquals("/api/portes/55", detailRequest.getPath());
    }

    @Test
    public void getOffers_parsesCurrentBackendPorteContract() throws Exception {
        server.enqueue(jsonResponse(200, "[{\"id\":3,\"origen\":\"Bilbao\",\"destino\":\"Zaragoza\",\"estado\":\"PENDIENTE\",\"descripcionCliente\":\"Materiales de construccion\",\"requiereFrio\":true,\"distanciaKm\":305.0,\"precioFinal\":520.0}]"));

        PorteRepository repository = new PorteRepository(createApiService());
        CallbackCapture<List<Porte>> capture = new CallbackCapture<>();

        repository.getOffers(1L, capture);

        RepositoryResult<List<Porte>> result = capture.await();
        assertTrue(result.isSuccessful());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        Porte porte = result.getData().get(0);
        assertEquals("Materiales de construccion", porte.getDescripcionMercancia());
        assertEquals(Boolean.TRUE, porte.getRequiereFrio());
        assertEquals(Double.valueOf(520.0), porte.getPrecio());

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/api/portes/ofertas/1", request.getPath());
    }

    @Test
    public void getPorteDetail_forbiddenResponseReturnsErrorWithoutCrashingCallback() throws Exception {
        server.enqueue(jsonResponse(403, "{\"message\":\"No autorizado para ver este porte\"}"));

        PorteRepository repository = new PorteRepository(createApiService());
        CallbackCapture<Porte> capture = new CallbackCapture<>();

        repository.getPorteDetail(99L, capture);

        RepositoryResult<Porte> result = capture.await();
        assertFalse(result.isSuccessful());
        assertFalse(result.isUnauthorized());
        assertEquals(403, result.getCode());

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/api/portes/99", request.getPath());
    }

    @Test
    public void rejectOffer_postsDriverDecisionWithoutFollowupFetch() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));

        PorteRepository repository = new PorteRepository(createApiService());
        CallbackCapture<Void> capture = new CallbackCapture<>();

        repository.rejectOffer(55L, 9L, capture);

        RepositoryResult<Void> result = capture.await();
        assertTrue(result.isSuccessful());

        RecordedRequest rejectRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(rejectRequest);
        assertEquals("POST", rejectRequest.getMethod());
        assertEquals("/api/portes/55/rechazar?conductorId=9", rejectRequest.getPath());
    }

    @Test
    public void changeTripState_refreshesDetailAfterMutation() throws Exception {
        server.enqueue(jsonResponse(200, "{\"id\":44,\"estado\":\"EN_TRANSITO\"}"));
        server.enqueue(jsonResponse(200, "{\"id\":44,\"origen\":\"Sevilla\",\"destino\":\"Malaga\",\"estado\":\"EN_TRANSITO\"}"));

        PorteRepository repository = new PorteRepository(createApiService());
        CallbackCapture<Porte> capture = new CallbackCapture<>();

        repository.changeTripState(44L, EstadoPorte.ASIGNADO, EstadoPorte.EN_TRANSITO, capture);

        RepositoryResult<Porte> result = capture.await();
        assertTrue(result.isSuccessful());
        assertNotNull(result.getData());
        assertEquals(EstadoPorte.EN_TRANSITO, result.getData().getEstadoPorte());

        RecordedRequest updateRequest = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest detailRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(updateRequest);
        assertNotNull(detailRequest);
        assertEquals("PUT", updateRequest.getMethod());
        assertEquals("/api/portes/44/estado?nuevo=EN_TRANSITO", updateRequest.getPath());
        assertEquals("GET", detailRequest.getMethod());
        assertEquals("/api/portes/44", detailRequest.getPath());
    }

    @Test
    public void upsertLocation_postsTrackingPayload() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));

        TrackingRepository repository = new TrackingRepository(createApiService());
        CallbackCapture<Void> capture = new CallbackCapture<>();

        repository.upsertLocation(11L, new DriverLocationUpdateRequest(
                40.4168,
                -3.7038,
                "2026-03-27T20:30:00Z",
                52.5,
                180
        ), capture);

        RepositoryResult<Void> result = capture.await();
        assertTrue(result.isSuccessful());

        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/tracking/drivers/11/locations", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"lat\":40.4168"));
        assertTrue(body.contains("\"lon\":-3.7038"));
        assertTrue(body.contains("\"recordedAt\":\"2026-03-27T20:30:00Z\""));
    }

    @Test
    public void upsertLocation_fallbacksToLegacyEndpointWhenRealtimeRouteMissing() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse().setResponseCode(200));

        TrackingRepository repository = new TrackingRepository(createApiService());
        CallbackCapture<Void> capture = new CallbackCapture<>();

        repository.upsertLocation(11L, new DriverLocationUpdateRequest(
                40.4168,
                -3.7038,
                "2026-03-27T20:30:00Z",
                52.5,
                180
        ), capture);

        RepositoryResult<Void> result = capture.await();
        assertTrue(result.isSuccessful());

        RecordedRequest realtimeRequest = server.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest legacyRequest = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(realtimeRequest);
        assertNotNull(legacyRequest);
        assertEquals("POST", realtimeRequest.getMethod());
        assertEquals("/api/v1/tracking/drivers/11/locations", realtimeRequest.getPath());
        assertEquals("POST", legacyRequest.getMethod());
        assertEquals("/api/conductores/11/ubicacion?lat=40.4168&lon=-3.7038", legacyRequest.getPath());
    }

    private MockResponse jsonResponse(int code, String body) {
        return new MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private com.cargohub.mobile.network.ApiService createApiService() {
        return new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(com.cargohub.mobile.network.ApiService.class);
    }

    private static final class CallbackCapture<T> implements RepositoryCallback<T> {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<RepositoryResult<T>> resultRef = new AtomicReference<>();

        @Override
        public void onResult(RepositoryResult<T> result) {
            resultRef.set(result);
            latch.countDown();
        }

        RepositoryResult<T> await() throws InterruptedException {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            return resultRef.get();
        }
    }
}
