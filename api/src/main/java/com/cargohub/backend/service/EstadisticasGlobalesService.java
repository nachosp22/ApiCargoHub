package com.cargohub.backend.service;

import com.cargohub.backend.dto.EstadisticasGlobalesResponse;
import com.cargohub.backend.dto.EstadisticasGlobalesResponse.*;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.repository.ClienteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.FacturaRepository;
import com.cargohub.backend.repository.PorteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EstadisticasGlobalesService {

    @Autowired
    private PorteRepository porteRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private ConductorRepository conductorRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    /**
     * Obtiene todas las estadísticas globales para el panel de administración.
     * <p>
     * Calcula los KPIs principales (total de portes, ingresos, conductores activos, clientes),
     * las tendencias mensuales comparando el mes actual con el anterior, la distribución
     * de portes por estado, el resumen de facturas (emitidas, pagadas, pendientes),
     * el top 5 de conductores por portes completados, el top 5 de clientes por facturación,
     * la evolución mensual de portes e ingresos de los últimos 12 meses y el desglose
     * completo de portes por cada estado existente.
     *
     * @return objeto {@link EstadisticasGlobalesResponse} con todas las métricas calculadas
     */
    public EstadisticasGlobalesResponse getEstadisticasGlobales() {
        EstadisticasGlobalesResponse resp = new EstadisticasGlobalesResponse();

        List<Porte> allPortes = porteRepository.findAll();
        List<Factura> allFacturas = facturaRepository.findAll();
        List<Conductor> allConductores = conductorRepository.findAll();

        // --- KPIs principales ---
        resp.setTotalPortes(allPortes.size());

        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(now);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        long portesEsteMes = allPortes.stream()
                .filter(p -> p.getFechaCreacion() != null
                        && YearMonth.from(p.getFechaCreacion().toLocalDate()).equals(currentMonth))
                .count();
        long portesMesAnterior = allPortes.stream()
                .filter(p -> p.getFechaCreacion() != null
                        && YearMonth.from(p.getFechaCreacion().toLocalDate()).equals(previousMonth))
                .count();

        resp.setPortesEsteMes(portesEsteMes);
        resp.setPortesTendencia(calcTendencia(portesEsteMes, portesMesAnterior));

        // Ingresos
        double totalIngresos = allFacturas.stream()
                .mapToDouble(f -> f.getImporteTotal() != null ? f.getImporteTotal() : 0.0)
                .sum();
        double ingresosEsteMes = allFacturas.stream()
                .filter(f -> f.getFechaEmision() != null
                        && YearMonth.from(f.getFechaEmision()).equals(currentMonth))
                .mapToDouble(f -> f.getImporteTotal() != null ? f.getImporteTotal() : 0.0)
                .sum();
        double ingresosMesAnterior = allFacturas.stream()
                .filter(f -> f.getFechaEmision() != null
                        && YearMonth.from(f.getFechaEmision()).equals(previousMonth))
                .mapToDouble(f -> f.getImporteTotal() != null ? f.getImporteTotal() : 0.0)
                .sum();

        resp.setTotalIngresos(round2(totalIngresos));
        resp.setIngresosEsteMes(round2(ingresosEsteMes));
        resp.setIngresosTendencia(calcTendencia(ingresosEsteMes, ingresosMesAnterior));

        // Conductores activos y clientes
        long conductoresActivos = allConductores.stream()
                .filter(Conductor::isDisponible)
                .count();
        resp.setTotalConductoresActivos(conductoresActivos);
        resp.setTotalClientes(clienteRepository.count());

        // --- Portes por estado ---
        long completados = allPortes.stream()
                .filter(p -> p.getEstado() == EstadoPorte.ENTREGADO || p.getEstado() == EstadoPorte.FACTURADO)
                .count();
        long pendientes = allPortes.stream()
                .filter(p -> p.getEstado() == EstadoPorte.PENDIENTE || p.getEstado() == EstadoPorte.ASIGNADO)
                .count();
        long enTransito = allPortes.stream()
                .filter(p -> p.getEstado() == EstadoPorte.EN_TRANSITO)
                .count();

        resp.setPortesCompletados(completados);
        resp.setPortesPendientes(pendientes);
        resp.setPortesEnTransito(enTransito);

        // --- Facturas ---
        resp.setFacturasEmitidas(allFacturas.size());
        resp.setFacturasPagadas(allFacturas.stream().filter(Factura::isPagada).count());
        resp.setFacturasPendientes(allFacturas.stream().filter(f -> !f.isPagada()).count());

        // --- Top 5 Conductores (by portes completados) ---
        Map<Conductor, Long> portesPorConductor = allPortes.stream()
                .filter(p -> p.getConductor() != null
                        && (p.getEstado() == EstadoPorte.ENTREGADO || p.getEstado() == EstadoPorte.FACTURADO))
                .collect(Collectors.groupingBy(Porte::getConductor, Collectors.counting()));

        List<TopConductor> topConductores = portesPorConductor.entrySet().stream()
                .sorted(Map.Entry.<Conductor, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new TopConductor(
                        e.getKey().getNombre() + " " + (e.getKey().getApellidos() != null ? e.getKey().getApellidos() : ""),
                        e.getValue()
                ))
                .collect(Collectors.toList());
        resp.setTopConductores(topConductores);

        // --- Top 5 Clientes (by facturado) ---
        Map<Long, double[]> clienteStats = new HashMap<>(); // [totalFacturado, portes]
        Map<Long, String> clienteNames = new HashMap<>();

        for (Factura f : allFacturas) {
            if (f.getPorte() != null && f.getPorte().getCliente() != null) {
                Long clienteId = f.getPorte().getCliente().getId();
                clienteNames.putIfAbsent(clienteId, f.getPorte().getCliente().getNombreEmpresa());
                double[] stats = clienteStats.computeIfAbsent(clienteId, k -> new double[]{0.0, 0.0});
                stats[0] += f.getImporteTotal() != null ? f.getImporteTotal() : 0.0;
                stats[1] += 1;
            }
        }

        List<TopCliente> topClientes = clienteStats.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]))
                .limit(5)
                .map(e -> new TopCliente(
                        clienteNames.getOrDefault(e.getKey(), "—"),
                        round2(e.getValue()[0]),
                        (long) e.getValue()[1]
                ))
                .collect(Collectors.toList());
        resp.setTopClientes(topClientes);

        // --- Portes por mes (últimos 12 meses) ---
        DateTimeFormatter mesFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        List<PorteMensual> portesPorMes = new ArrayList<>();

        for (int i = 11; i >= 0; i--) {
            YearMonth ym = currentMonth.minusMonths(i);
            String mesKey = ym.format(mesFormatter);

            long cantidad = allPortes.stream()
                    .filter(p -> p.getFechaCreacion() != null
                            && YearMonth.from(p.getFechaCreacion().toLocalDate()).equals(ym))
                    .count();

            double ingresos = allFacturas.stream()
                    .filter(f -> f.getFechaEmision() != null
                            && YearMonth.from(f.getFechaEmision()).equals(ym))
                    .mapToDouble(f -> f.getImporteTotal() != null ? f.getImporteTotal() : 0.0)
                    .sum();

            portesPorMes.add(new PorteMensual(mesKey, cantidad, round2(ingresos)));
        }
        resp.setPortesPorMes(portesPorMes);

        // --- Portes por estado (all) ---
        List<PorteEstado> portesPorEstado = allPortes.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getEstado() != null ? p.getEstado().name() : "DESCONOCIDO",
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> new PorteEstado(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PorteEstado::getEstado))
                .collect(Collectors.toList());
        resp.setPortesPorEstado(portesPorEstado);

        return resp;
    }

    /**
     * Calcula el porcentaje de variación entre el valor actual y el anterior.
     *
     * @param actual   valor del período actual
     * @param anterior valor del período anterior
     * @return porcentaje de tendencia redondeado a 2 decimales (100.0 si anterior es 0 y actual > 0, 0.0 en caso contrario)
     */
    private double calcTendencia(double actual, double anterior) {
        if (anterior == 0) return actual > 0 ? 100.0 : 0.0;
        return round2(((actual - anterior) / anterior) * 100.0);
    }

    /**
     * Redondea un valor double a dos decimales.
     *
     * @param val valor a redondear
     * @return valor redondeado a 2 decimales
     */
    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
