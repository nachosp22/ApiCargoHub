package com.cargohub.backend.service;

import com.cargohub.backend.entity.BloqueoRecurrente;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.repository.BloqueoRecurrenteRepository;
import com.cargohub.backend.repository.ConductorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BloqueoRecurrenteService {

    private static final String[] NOMBRES_DIAS = {
            "", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };

    @Autowired
    private BloqueoRecurrenteRepository bloqueoRecurrenteRepository;

    @Autowired
    private ConductorRepository conductorRepository;

    /**
     * Devuelve los 7 días con su estado activo/inactivo para un conductor.
     */
    public List<BloqueoRecurrenteResponse> getByConductor(Long conductorId) {
        Map<Integer, BloqueoRecurrente> existentes = bloqueoRecurrenteRepository
                .findByConductorId(conductorId)
                .stream()
                .collect(Collectors.toMap(BloqueoRecurrente::getDiaSemana, Function.identity()));

        List<BloqueoRecurrenteResponse> resultado = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            BloqueoRecurrente bloqueo = existentes.get(dia);
            boolean activo = bloqueo != null && bloqueo.isActivo();
            resultado.add(new BloqueoRecurrenteResponse(dia, NOMBRES_DIAS[dia], activo));
        }
        return resultado;
    }

    /**
     * Activa o desactiva un día específico.
     */
    @Transactional
    public void toggleDia(Long conductorId, int diaSemana, boolean activo) {
        validarDiaSemana(diaSemana);
        BloqueoRecurrente bloqueo = bloqueoRecurrenteRepository
                .findByConductorIdAndDiaSemana(conductorId, diaSemana)
                .orElseGet(() -> {
                    BloqueoRecurrente nuevo = new BloqueoRecurrente();
                    Conductor c = conductorRepository.findById(conductorId)
                            .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));
                    nuevo.setConductor(c);
                    nuevo.setDiaSemana(diaSemana);
                    return nuevo;
                });
        bloqueo.setActivo(activo);
        bloqueoRecurrenteRepository.save(bloqueo);
    }

    /**
     * Setea todos los días bloqueados de golpe.
     * Recibe la lista de días que deben estar bloqueados (activo=true).
     * Los demás se desactivan.
     */
    @Transactional
    public List<BloqueoRecurrenteResponse> setBulk(Long conductorId, List<Integer> diasBloqueados) {
        Conductor conductor = conductorRepository.findById(conductorId)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));

        Map<Integer, BloqueoRecurrente> existentes = bloqueoRecurrenteRepository
                .findByConductorId(conductorId)
                .stream()
                .collect(Collectors.toMap(BloqueoRecurrente::getDiaSemana, Function.identity()));

        for (int dia = 1; dia <= 7; dia++) {
            boolean debeEstarActivo = diasBloqueados.contains(dia);
            BloqueoRecurrente bloqueo = existentes.get(dia);
            if (bloqueo == null) {
                bloqueo = new BloqueoRecurrente();
                bloqueo.setConductor(conductor);
                bloqueo.setDiaSemana(dia);
            }
            bloqueo.setActivo(debeEstarActivo);
            bloqueoRecurrenteRepository.save(bloqueo);
        }

        return getByConductor(conductorId);
    }

    private void validarDiaSemana(int diaSemana) {
        if (diaSemana < 1 || diaSemana > 7) {
            throw new IllegalArgumentException("diaSemana debe estar entre 1 (Lunes) y 7 (Domingo)");
        }
    }

    /**
     * DTO de respuesta para cada día de la semana.
     */
    public record BloqueoRecurrenteResponse(int diaSemana, String nombre, boolean activo) {
    }
}
