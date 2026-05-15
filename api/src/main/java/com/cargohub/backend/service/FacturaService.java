package com.cargohub.backend.service;

import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.FacturaRepository;
import com.cargohub.backend.repository.PorteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cargohub.backend.dto.FacturaResumenResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FacturaService {

    private static final String DEFAULT_FACTURA_PREFIX = "F";
    private static final int DEFAULT_SEQUENCE_PADDING = 4;
    private static final Pattern FACTURA_CODE_PATTERN = Pattern.compile("^([A-Za-z]+)-(\\d{4})-([A-Za-z]*)(\\d+)$");

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private PorteRepository porteRepository;

    /**
     * Genera la factura definitiva para un porte entregado.
     * Se llama cuando el Admin pulsa "Facturar" manualmente.
     *
     * @param porteId el identificador único del porte a facturar
     * @return la entidad {@link Factura} recién creada y persistida en base de datos
     * @throws RuntimeException si el porte no existe o ya tiene una factura asociada
     */
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

        // Número de serie: AÑO_PORTE (ej: 2026_00054)
        int anio = LocalDate.now().getYear();
        factura.setNumeroSerie(String.format("%d_%05d", anio, porteId));

        return facturaRepository.save(factura);
    }

    /**
     * Calcula el siguiente código de factura con formato secuencial por año.
     * Ej: Si la última fue F-2025-0009, devuelve F-2025-0010.
     *
     * @return el siguiente número de serie de factura como cadena de texto
     */
    private String generarSiguienteNumeroFactura() {
        int anioActual = LocalDate.now().getYear();

        // Buscamos la última factura creada en la base de datos
        Optional<Factura> ultimaFactura = facturaRepository.findTopByOrderByIdDesc();

        int secuencia = 1; // Por defecto empezamos en 1
        String prefijoFactura = DEFAULT_FACTURA_PREFIX;
        String prefijoSecuencia = "";
        int longitudSecuencia = DEFAULT_SEQUENCE_PADDING;

        if (ultimaFactura.isPresent()) {
            String ultimoCodigo = ultimaFactura.get().getNumeroSerie();
            Optional<FacturaCodeParts> parsed = parseFacturaCode(ultimoCodigo);

            if (parsed.isPresent()) {
                FacturaCodeParts last = parsed.get();
                prefijoFactura = last.prefijoFactura();
                prefijoSecuencia = last.prefijoSecuencia();
                longitudSecuencia = Math.max(last.longitudSecuencia(), DEFAULT_SEQUENCE_PADDING);

                // Si seguimos en el mismo año, sumamos 1.
                // Si hemos cambiado de año (ej: 2026), se reinicia a 1.
                if (last.anio() == anioActual) {
                    secuencia = last.secuencia() + 1;
                }
            }
        }

        // Formateamos con 4 dígitos de relleno (padding)
        String secuenciaFormateada = String.format("%0" + longitudSecuencia + "d", secuencia);
        return String.format("%s-%d-%s%s", prefijoFactura, anioActual, prefijoSecuencia, secuenciaFormateada);
    }

    /**
     * Analiza un código de factura y extrae sus componentes estructurales (prefijo, año, prefijo de secuencia, secuencia y longitud).
     *
     * @param codigo la cadena de texto con el número de serie a parsear
     * @return un {@link Optional} con los componentes extraídos, o vacío si el código es nulo, vacío o no coincide con el patrón esperado
     */
    private Optional<FacturaCodeParts> parseFacturaCode(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = FACTURA_CODE_PATTERN.matcher(codigo.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            String prefijoFactura = matcher.group(1).toUpperCase();
            int anio = Integer.parseInt(matcher.group(2));
            String prefijoSecuencia = matcher.group(3).toUpperCase();
            String secuenciaRaw = matcher.group(4);
            int secuencia = Integer.parseInt(secuenciaRaw);

            return Optional.of(new FacturaCodeParts(prefijoFactura, anio, prefijoSecuencia, secuencia, secuenciaRaw.length()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private record FacturaCodeParts(String prefijoFactura, int anio, String prefijoSecuencia, int secuencia,
                                    int longitudSecuencia) {
    }

    // ── Métodos para conductor ──

    /**
     * Busca las facturas asociadas a un conductor aplicando filtros opcionales de fecha y estado de pago.
     *
     * @param conductorId el identificador único del conductor cuyas facturas se desean consultar
     * @param desde la fecha de inicio del rango de búsqueda (inclusive), o {@code null} para sin límite inferior
     * @param hasta la fecha de fin del rango de búsqueda (inclusive), o {@code null} para sin límite superior
     * @param pagada filtro por estado de pago: {@code true} para facturas pagadas, {@code false} para pendientes, o {@code null} para sin filtro
     * @param pageable la configuración de paginación y ordenación
     * @return una página con las facturas que cumplen los criterios de búsqueda
     */
    public Page<Factura> findByConductorId(Long conductorId, LocalDate desde, LocalDate hasta,
                                            Boolean pagada, Pageable pageable) {
        return facturaRepository.findByConductorId(conductorId, desde, hasta, pagada, pageable);
    }

    /**
     * Obtiene un resumen financiero de las facturas de un conductor para un periodo determinado.
     * El resumen incluye el total facturado, el total pagado, el total pendiente y el número de facturas.
     *
     * @param conductorId el identificador único del conductor
     * @param periodo el rango de tiempo a considerar: "SEMANA" (desde el lunes anterior), "MES" (desde el día 1), "ANIO" (desde el 1 de enero), o {@code null} para sin límite inferior
     * @return un objeto {@link FacturaResumenResponse} con los totales calculados y redondeados a dos decimales
     */
    public FacturaResumenResponse getResumenByConductor(Long conductorId, String periodo) {
        LocalDate desde = null;
        LocalDate hasta = LocalDate.now();

        if (periodo != null) {
            switch (periodo.toUpperCase()) {
                case "SEMANA":
                    desde = hasta.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    break;
                case "MES":
                    desde = hasta.withDayOfMonth(1);
                    break;
                case "ANIO":
                    desde = hasta.withDayOfYear(1);
                    break;
                default:
                    break;
            }
        }

        List<Factura> facturas = facturaRepository.findAllByConductorId(conductorId, desde, hasta);

        double totalFacturado = facturas.stream()
                .mapToDouble(f -> f.getImporteTotal() != null ? f.getImporteTotal() : 0.0)
                .sum();
        double totalPagado = facturas.stream()
                .filter(Factura::isPagada)
                .mapToDouble(f -> f.getImporteTotal() != null ? f.getImporteTotal() : 0.0)
                .sum();

        FacturaResumenResponse resumen = new FacturaResumenResponse();
        resumen.setTotalFacturado(Math.round(totalFacturado * 100.0) / 100.0);
        resumen.setTotalPagado(Math.round(totalPagado * 100.0) / 100.0);
        resumen.setTotalPendiente(Math.round((totalFacturado - totalPagado) * 100.0) / 100.0);
        resumen.setNumeroFacturas((long) facturas.size());
        return resumen;
    }
}
