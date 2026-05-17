package com.cargohub.backend.service;

import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.repository.FacturaRepository;
import com.cargohub.backend.repository.PorteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturaServiceNumeroSerieTest {

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private PorteRepository porteRepository;

    @InjectMocks
    private FacturaService facturaService;

    private Porte porte;

    @BeforeEach
    void setUp() {
        porte = new Porte();
        porte.setId(2L);
        porte.setPrecio(100.0);
        porte.setAjustePrecio(0.0);
    }

    @Test
    void generarFactura_paraCodigoSeedConPrefijoP_incrementaSinNumberFormatException() {
        Factura ultimaFactura = new Factura();
        int year = LocalDate.now().getYear();
        ultimaFactura.setNumeroSerie("SEED-" + year + "-P00012");

        when(porteRepository.findById(2L)).thenReturn(Optional.of(porte));
        when(facturaRepository.findByPorteId(2L)).thenReturn(Optional.empty());
        when(facturaRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(ultimaFactura));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

        facturaService.generarFacturaParaPorte(2L);

        ArgumentCaptor<Factura> facturaCaptor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(facturaCaptor.capture());
        assertEquals("SEED-" + year + "-P00013", facturaCaptor.getValue().getNumeroSerie());
    }

    @Test
    void generarFactura_paraFormatoFacturaFac_respetaPrefijoExistente() {
        Factura ultimaFactura = new Factura();
        int year = LocalDate.now().getYear();
        ultimaFactura.setNumeroSerie("FAC-" + year + "-0099");

        when(porteRepository.findById(2L)).thenReturn(Optional.of(porte));
        when(facturaRepository.findByPorteId(2L)).thenReturn(Optional.empty());
        when(facturaRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(ultimaFactura));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

        facturaService.generarFacturaParaPorte(2L);

        ArgumentCaptor<Factura> facturaCaptor = ArgumentCaptor.forClass(Factura.class);
        verify(facturaRepository).save(facturaCaptor.capture());
        assertEquals("FAC-" + year + "-0100", facturaCaptor.getValue().getNumeroSerie());
    }
}
