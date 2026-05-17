package com.cargohub.backend.entity;

import com.cargohub.backend.entity.enums.EstadoVehiculo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadoVehiculoTest {

    @Test
    void enumDebeIncluirEstadoEnMantenimiento() {
        assertEquals(EstadoVehiculo.EN_MANTENIMIENTO, EstadoVehiculo.valueOf("EN_MANTENIMIENTO"));
    }
}
