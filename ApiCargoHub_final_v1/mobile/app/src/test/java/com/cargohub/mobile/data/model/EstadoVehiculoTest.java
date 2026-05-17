package com.cargohub.mobile.data.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EstadoVehiculoTest {

    @Test
    public void enMantenimiento_tieneDisplayNameEsperado() {
        assertEquals("En mantenimiento", EstadoVehiculo.EN_MANTENIMIENTO.getDisplayName());
    }
}
