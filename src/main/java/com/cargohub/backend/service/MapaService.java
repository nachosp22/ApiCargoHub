package com.cargohub.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.List;

@Service
public class MapaService {

    // Cliente HTTP de Spring para llamar a APIs externas
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Pregunta a OSRM cuántos metros hay de verdad por carretera.
     * Retorna NULL si falla la conexión (para tener un plan B).
     */
    public Double obtenerDistanciaMetros(Double latOrigen, Double lonOrigen, Double latDestino, Double lonDestino) {
        // URL pública de OSRM (Gratuita)
        // Formato: /driving/longitud,latitud;longitud,latitud
        String url = String.format(
                "http://router.project-osrm.org/route/v1/driving/%s,%s;%s,%s?overview=false",
                lonOrigen, latOrigen, lonDestino, latDestino
        );

        try {
            ResponseEntity<Map> respuesta = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = respuesta.getBody();

            if (body != null && body.containsKey("routes")) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");
                if (!routes.isEmpty()) {
                    // OSRM devuelve la distancia en metros
                    Object distancia = routes.get(0).get("distance");
                    return Double.valueOf(distancia.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("Error conectando con OSRM: " + e.getMessage());
            // Si falla, devolvemos null y usaremos Haversine como fallback
        }
        return null;
    }
}