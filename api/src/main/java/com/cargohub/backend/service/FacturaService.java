package com.cargohub.backend.service;

import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.FacturaRepository;
import com.cargohub.backend.repository.PorteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class FacturaService {

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private PorteRepository porteRepository;

    /**
     * Genera la factura definitiva para un porte entregado.
     * Se llama cuando el Admin pulsa "Facturar" manualmente.
     */
    @Transactional
    public Factura generarFacturaParaPorte(Long porteId) {
        Porte porte = porteRepository.findById(porteId)
                .orElseThrow(() -> new RuntimeException("Porte no encontrado"));

        // Comprobación de seguridad para no duplicar facturas
        if (facturaRepository.findByPorteId(porteId).isPresent()) {
            throw new RuntimeException("Este porte ya tiene una factura asociada.");
        }

        Factura factura = new Factura();
        factura.setPorte(porte);

        // --- CAMBIO CLAVE: COBRAR EL TOTAL REAL ---
        // Usamos getPrecioFinal() para incluir los ajustes manuales (penalizaciones/extras).
        // Si el precio base eran 100€ y metiste -20€ de penalización, la base imponible será 80€.
        factura.setBaseImponible(porte.getPrecioFinal());

        // El IVA y el Importe Total se calculan automáticamente
        // gracias al @PrePersist que pusimos en la entidad Factura.

        // Generamos el número secuencial (ej: F-2025-0042)
        factura.setNumeroSerie(generarSiguienteNumeroFactura());

        return facturaRepository.save(factura);
    }

    /**
     * Calcula el siguiente código de factura: F-{AÑO}-{SECUENCIA}
     * Ej: Si la última fue F-2025-0009, devuelve F-2025-0010.
     */
    private String generarSiguienteNumeroFactura() {
        int anioActual = LocalDate.now().getYear();

        // Buscamos la última factura creada en la base de datos
        Optional<Factura> ultimaFactura = facturaRepository.findTopByOrderByIdDesc();

        int secuencia = 1; // Por defecto empezamos en 1

        if (ultimaFactura.isPresent()) {
            String ultimoCodigo = ultimaFactura.get().getNumeroSerie();
            // Parseamos el código: F-2025-0001
            String[] partes = ultimoCodigo.split("-");

            if (partes.length == 3) {
                int anioUltima = Integer.parseInt(partes[1]);
                int numeroUltima = Integer.parseInt(partes[2]);

                // Si seguimos en el mismo año, sumamos 1.
                // Si hemos cambiado de año (ej: 2026), se reinicia a 1.
                if (anioUltima == anioActual) {
                    secuencia = numeroUltima + 1;
                }
            }
        }

        // Formateamos con 4 dígitos de relleno (padding)
        return String.format("F-%d-%04d", anioActual, secuencia);
    }
}