package com.cargohub.backend.service;

import com.cargohub.backend.dto.EstadisticasConductorResponse;
import com.cargohub.backend.dto.EstadisticasConductorResponse.IngresoMensual;
import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.repository.FacturaRepository;
import com.cargohub.backend.repository.PorteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EstadisticasConductorService {

    @Autowired
    private PorteRepository porteRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    public EstadisticasConductorResponse getEstadisticas(Long conductorId,
                                                          LocalDate desde,
                                                          LocalDate hasta) {
        LocalDateTime desdedt = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDt = hasta != null ? hasta.atTime(23, 59, 59) : null;

        List<Porte> portes = porteRepository.findByConductorIdAndFechas(conductorId, desdedt, hastaDt);
        List<Factura> facturas = facturaRepository.findAllByConductorId(conductorId, desde, hasta);

        EstadisticasConductorResponse resp = new EstadisticasConductorResponse();

        // Portes por estado conteo
        long completados = portes.stream()
                .filter(p -> p.getEstado() == EstadoPorte.ENTREGADO || p.getEstado() == EstadoPorte.FACTURADO)
                .count();
        long cancelados = portes.stream()
                .filter(p -> p.getEstado() == EstadoPorte.CANCELADO)
                .count();
        long enCurso = portes.stream()
                .filter(p -> p.getEstado() == EstadoPorte.EN_TRANSITO)
                .count();
        long pendientes = portes.stream()
                .filter(p -> p.getEstado() == EstadoPorte.PENDIENTE || p.getEstado() == EstadoPorte.ASIGNADO)
                .count();

        resp.setPortesCompletados(completados);
        resp.setPortesCancelados(cancelados);
        resp.setPortesEnCurso(enCurso);
        resp.setPortesPendientes(pendientes);

        // Km recorridos
        double km = portes.stream()
                .filter(p -> p.getDistanciaKm() != null)
                .mapToDouble(Porte::getDistanciaKm)
                .sum();
        resp.setKmRecorridos(Math.round(km * 100.0) / 100.0);

        // Ingreso total (de facturas)
        double ingresoTotal = facturas.stream()
                .mapToDouble(f -> f.getImporteTotal() != null ? f.getImporteTotal() : 0.0)
                .sum();
        resp.setIngresoTotal(Math.round(ingresoTotal * 100.0) / 100.0);

        // Media por porte
        resp.setMediaPorPorte(completados > 0
                ? Math.round((ingresoTotal / completados) * 100.0) / 100.0
                : 0.0);

        // Ingreso por mes (últimos 12 meses) con formato "YYYY-MM" y conteo de portes
        DateTimeFormatter mesFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        List<IngresoMensual> ingresoMes = new ArrayList<>();
        LocalDate ahora = LocalDate.now();
        for (int i = 11; i >= 0; i--) {
            LocalDate mesRef = ahora.minusMonths(i).withDayOfMonth(1);
            int anio = mesRef.getYear();
            int mes = mesRef.getMonthValue();
            String mesKey = mesRef.format(mesFormatter);

            double totalMes = facturas.stream()
                    .filter(f -> f.getFechaEmision() != null
                            && f.getFechaEmision().getYear() == anio
                            && f.getFechaEmision().getMonthValue() == mes)
                    .mapToDouble(f -> f.getImporteTotal() != null ? f.getImporteTotal() : 0.0)
                    .sum();

            long portesMes = portes.stream()
                    .filter(p -> p.getFechaCreacion() != null
                            && p.getFechaCreacion().getYear() == anio
                            && p.getFechaCreacion().getMonthValue() == mes)
                    .count();

            ingresoMes.add(new IngresoMensual(mesKey, Math.round(totalMes * 100.0) / 100.0, portesMes));
        }
        resp.setIngresoPorMes(ingresoMes);

        // Portes por estado
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (Porte p : portes) {
            String estado = p.getEstado() != null ? p.getEstado().name() : "DESCONOCIDO";
            porEstado.merge(estado, 1L, Long::sum);
        }
        resp.setPortesPorEstado(porEstado);

        return resp;
    }
}
