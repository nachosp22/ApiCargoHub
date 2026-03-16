package com.cargohub.backend.service;

import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CalculadoraPrecioServiceTest {

    @InjectMocks
    private CalculadoraPrecioService calculadoraPrecioService;

    @Test
    void testCalcularPrecioTotal_Furgoneta() {
        // Given
        Porte porte = new Porte();
        porte.setDistanciaKm(100.0);
        porte.setTipoVehiculoRequerido(TipoVehiculo.FURGONETA);
        porte.setFechaRecogida(LocalDateTime.of(2024, 1, 15, 10, 0));
        porte.setFechaEntrega(LocalDateTime.of(2024, 1, 15, 15, 0));

        // When
        Double precio = calculadoraPrecioService.calcularPrecioTotal(porte);

        // Then
        assertNotNull(precio);
        // Precio base: 100km * 0.90€/km + 20€ fijo = 110€
        assertEquals(110.0, precio);
    }

    @Test
    void testCalcularPrecioTotal_ConTarifaMinima() {
        // Given
        Porte porte = new Porte();
        porte.setDistanciaKm(10.0);  // Distancia corta
        porte.setTipoVehiculoRequerido(TipoVehiculo.FURGONETA);
        porte.setFechaRecogida(LocalDateTime.of(2024, 1, 15, 10, 0));
        porte.setFechaEntrega(LocalDateTime.of(2024, 1, 15, 12, 0));

        // When
        Double precio = calculadoraPrecioService.calcularPrecioTotal(porte);

        // Then
        assertNotNull(precio);
        // 10km * 0.90€/km + 20€ = 29€, pero mínimo furgoneta es 40€
        assertEquals(40.0, precio);
    }

    @Test
    void testCalcularPrecioTotal_HorarioNocturno() {
        // Given
        Porte porte = new Porte();
        porte.setDistanciaKm(100.0);
        porte.setTipoVehiculoRequerido(TipoVehiculo.RIGIDO);
        porte.setFechaRecogida(LocalDateTime.of(2024, 1, 15, 23, 0)); // Nocturno
        porte.setFechaEntrega(LocalDateTime.of(2024, 1, 16, 4, 0));

        // When
        Double precio = calculadoraPrecioService.calcularPrecioTotal(porte);

        // Then
        assertNotNull(precio);
        // Base: 100km * 1.40€/km + 20€ = 160€
        // Con plus nocturno (20%): 160€ * 1.20 = 192€
        assertEquals(192.0, precio);
    }

    @Test
    void testCalcularPrecioTotal_FinDeSemana() {
        // Given
        Porte porte = new Porte();
        porte.setDistanciaKm(100.0);
        porte.setTipoVehiculoRequerido(TipoVehiculo.RIGIDO);
        porte.setFechaRecogida(LocalDateTime.of(2024, 1, 20, 10, 0)); // Sábado
        porte.setFechaEntrega(LocalDateTime.of(2024, 1, 20, 15, 0));

        // When
        Double precio = calculadoraPrecioService.calcularPrecioTotal(porte);

        // Then
        assertNotNull(precio);
        // Base: 100km * 1.40€/km + 20€ = 160€
        // Con plus fin de semana (25%): 160€ * 1.25 = 200€
        assertEquals(200.0, precio);
    }

    @Test
    void testCalcularPrecioTotal_Trailer() {
        // Given
        Porte porte = new Porte();
        porte.setDistanciaKm(200.0);
        porte.setTipoVehiculoRequerido(TipoVehiculo.TRAILER);
        porte.setFechaRecogida(LocalDateTime.of(2024, 1, 15, 10, 0));
        porte.setFechaEntrega(LocalDateTime.of(2024, 1, 15, 18, 0));

        // When
        Double precio = calculadoraPrecioService.calcularPrecioTotal(porte);

        // Then
        assertNotNull(precio);
        // 200km * 1.65€/km + 20€ = 350€
        assertEquals(350.0, precio);
    }

    @Test
    void testCalcularPrecioTotal_DistanciaCero() {
        // Given
        Porte porte = new Porte();
        porte.setDistanciaKm(0.0);
        porte.setTipoVehiculoRequerido(TipoVehiculo.FURGONETA);
        porte.setFechaRecogida(LocalDateTime.of(2024, 1, 15, 10, 0));

        // When
        Double precio = calculadoraPrecioService.calcularPrecioTotal(porte);

        // Then
        assertEquals(0.0, precio);
    }

    @Test
    void testCalcularPrecioTotal_DistanciaNula() {
        // Given
        Porte porte = new Porte();
        porte.setDistanciaKm(null);
        porte.setTipoVehiculoRequerido(TipoVehiculo.FURGONETA);

        // When
        Double precio = calculadoraPrecioService.calcularPrecioTotal(porte);

        // Then
        assertEquals(0.0, precio);
    }

    @Test
    void testCalcularPrecioTotal_NocturnoYFinDeSemana() {
        // Given
        Porte porte = new Porte();
        porte.setDistanciaKm(100.0);
        porte.setTipoVehiculoRequerido(TipoVehiculo.RIGIDO);
        porte.setFechaRecogida(LocalDateTime.of(2024, 1, 20, 23, 0)); // Sábado nocturno
        porte.setFechaEntrega(LocalDateTime.of(2024, 1, 21, 2, 0));

        // When
        Double precio = calculadoraPrecioService.calcularPrecioTotal(porte);

        // Then
        assertNotNull(precio);
        // Base: 100km * 1.40€/km + 20€ = 160€
        // Con plus nocturno (20%) + fin de semana (25%): 160€ * 1.45 = 232€
        assertEquals(232.0, precio);
    }
}
