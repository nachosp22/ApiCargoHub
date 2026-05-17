package com.cargohub.backend.service;

import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Service
public class CalculadoraPrecioService {

    // --- PRECIOS BASE (€/KM) ---
    private static final double PRECIO_BASE_TRAILER = 1.65;
    private static final double PRECIO_BASE_RIGIDO = 1.40;
    private static final double PRECIO_BASE_FURGONETA = 0.90;

    // --- FIJO DE ARRANQUE (BAJADA DE BANDERA) ---
    private static final double FIJO_ARRANQUE = 20.0;

    // --- SUPLEMENTOS (PORCENTAJES) ---
    private static final double PLUS_NOCTURNO = 0.20;
    private static final double PLUS_FIN_SEMANA = 0.25;

    // --- TARIFAS MÍNIMAS (SEGÚN VEHÍCULO) ---
    private static final double MINIMO_TRAILER = 90.0;
    private static final double MINIMO_RIGIDO = 70.0;
    private static final double MINIMO_FURGONETA = 40.0;

    public Double calcularPrecioTotal(Porte porte) {
        if (porte.getDistanciaKm() == null || porte.getDistanciaKm() <= 0) {
            return 0.0;
        }

        // 1. Obtener datos según vehículo
        double precioKm = obtenerPrecioPorVehiculo(porte.getTipoVehiculoRequerido());
        double tarifaMinima = obtenerMinimoPorVehiculo(porte.getTipoVehiculoRequerido());

        // 2. Cálculo Estructural
        double precioTotal = (porte.getDistanciaKm() * precioKm) + FIJO_ARRANQUE;

        // 3. Aplicar SUPLEMENTOS
        double multiplicador = 1.0;

        if (esHorarioNocturno(porte.getFechaRecogida()) || esHorarioNocturno(porte.getFechaEntrega())) {
            multiplicador += PLUS_NOCTURNO;
        }

        if (esFinDeSemana(porte.getFechaRecogida()) || esFinDeSemana(porte.getFechaEntrega())) {
            multiplicador += PLUS_FIN_SEMANA;
        }

        precioTotal = precioTotal * multiplicador;

        // 4. Validar Tarifa Mínima Específica
        if (precioTotal < tarifaMinima) {
            precioTotal = tarifaMinima;
        }

        return Math.round(precioTotal * 100.0) / 100.0;
    }

    // --- Lógica Auxiliar ---

    private double obtenerPrecioPorVehiculo(TipoVehiculo tipo) {
        if (tipo == null) return PRECIO_BASE_RIGIDO;
        switch (tipo) {
            case TRAILER: return PRECIO_BASE_TRAILER;
            case RIGIDO: return PRECIO_BASE_RIGIDO;
            case FURGONETA: return PRECIO_BASE_FURGONETA;
            default: return PRECIO_BASE_RIGIDO;
        }
    }

    // NUEVO MÉTODO: Diferencia el coste mínimo
    private double obtenerMinimoPorVehiculo(TipoVehiculo tipo) {
        if (tipo == null) return MINIMO_RIGIDO;
        switch (tipo) {
            case TRAILER: return MINIMO_TRAILER;   // 90€
            case RIGIDO: return MINIMO_RIGIDO;     // 70€
            case FURGONETA: return MINIMO_FURGONETA; // 40€
            default: return MINIMO_RIGIDO;
        }
    }

    private boolean esHorarioNocturno(LocalDateTime fecha) {
        if (fecha == null) return false;
        int hora = fecha.getHour();
        return hora >= 22 || hora < 6;
    }

    private boolean esFinDeSemana(LocalDateTime fecha) {
        if (fecha == null) return false;
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }
}