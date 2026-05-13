package com.cargohub.backend.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VehiculoTest {

    @Test
    void calcularVolumenAutomatico_usesLongArithmeticForLargeDimensions() {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setLargoUtilMm(4200);
        vehiculo.setAnchoUtilMm(1800);
        vehiculo.setAltoUtilMm(1900);

        vehiculo.calcularVolumenAutomatico();

        assertEquals(14.36, vehiculo.getVolumenM3());
    }
}
