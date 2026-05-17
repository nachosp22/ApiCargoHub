package com.cargohub.backend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

@Slf4j
public class CalculadoraDistancia {

    private static final int RADIO_TIERRA_KM = 6371;
    private static final String OSRM_URL = "https://router.project-osrm.org/route/v1/driving/%s,%s;%s,%s?overview=false";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final RestClient restClient = RestClient.create();

    /**
     * Calcula la distancia por carretera usando OSRM (OpenStreetMap).
     * Si OSRM falla o no está disponible, usa Haversine × 1.2 como fallback.
     *
     * @return distancia en kilómetros
     */
    public static double calcularKm(double lat1, double lon1, double lat2, double lon2) {
        // 1. Intentar OSRM (distancia real por carretera)
        try {
            String url = String.format(OSRM_URL, lon1, lat1, lon2, lat2);
            String response = restClient.get()
                    .uri(url)
                    .header("User-Agent", "ApiCargoHub/1.0")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String code = root.path("code").asText();
            if ("Ok".equals(code)) {
                JsonNode routes = root.path("routes");
                if (routes.isArray() && !routes.isEmpty()) {
                    double metros = routes.get(0).path("distance").asDouble();
                    double km = metros / 1000.0;
                    log.debug("OSRM: distancia calculada = {} km", km);
                    return km;
                }
            }
            log.warn("OSRM responded but no valid route: code={}", code);
        } catch (Exception e) {
            log.warn("OSRM no disponible, usando Haversine como fallback: {}", e.getMessage());
        }

        // 2. Fallback: Haversine (línea recta × 1.2)
        double kmLineaRecta = haversine(lat1, lon1, lat2, lon2);
        double kmEstimado = kmLineaRecta * 1.2;
        log.debug("Haversine fallback: {} km (línea recta) × 1.2 = {} km", kmLineaRecta, kmEstimado);
        return kmEstimado;
    }

    /**
     * Fórmula de Haversine: distancia en línea recta entre dos puntos GPS.
     */
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }
}
