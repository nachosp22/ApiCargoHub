package com.cargohub.backend.service;

import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlbaranEntregaPdfServiceTest {

    private final AlbaranEntregaPdfService service = new AlbaranEntregaPdfService();

    @Test
    void generatePdf_conPorteValido_generaBytes() {
        Porte porte = new Porte();
        porte.setId(10L);
        porte.setEstado(EstadoPorte.ENTREGADO);
        porte.setFirmaEntregaBase64("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO8E2mQAAAAASUVORK5CYII=");
        porte.setFirmaEntregaFirmadoPor("Juan Pérez");

        byte[] pdf = service.generatePdf(porte);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generatePdf_sinFirma_oSinFirmante_oEstadoInvalido_fallaClaro() {
        Porte sinFirma = new Porte();
        sinFirma.setId(1L);
        sinFirma.setEstado(EstadoPorte.ENTREGADO);
        sinFirma.setFirmaEntregaFirmadoPor("Ana");
        RuntimeException e1 = assertThrows(RuntimeException.class, () -> service.generatePdf(sinFirma));
        assertTrue(e1.getMessage().contains("firma de entrega"));

        Porte sinFirmante = new Porte();
        sinFirmante.setId(2L);
        sinFirmante.setEstado(EstadoPorte.ENTREGADO);
        sinFirmante.setFirmaEntregaBase64("abc");
        RuntimeException e2 = assertThrows(RuntimeException.class, () -> service.generatePdf(sinFirmante));
        assertTrue(e2.getMessage().contains("firmante"));

        Porte estadoInvalido = new Porte();
        estadoInvalido.setId(3L);
        estadoInvalido.setEstado(EstadoPorte.PENDIENTE);
        estadoInvalido.setFirmaEntregaBase64("abc");
        estadoInvalido.setFirmaEntregaFirmadoPor("Ana");
        RuntimeException e3 = assertThrows(RuntimeException.class, () -> service.generatePdf(estadoInvalido));
        assertTrue(e3.getMessage().contains("entregados o facturados"));
    }
}
