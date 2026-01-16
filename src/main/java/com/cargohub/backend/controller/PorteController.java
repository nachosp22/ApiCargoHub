package com.cargohub.backend.controller;

import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.entity.Porte;
import com.cargohub.backend.entity.enums.EstadoPorte;
import com.cargohub.backend.service.PorteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portes")
@CrossOrigin(origins = "*")
public class PorteController {

    @Autowired
    private PorteService porteService;

    // 1. Crear Porte (Admin/IA)
    @PostMapping
    public ResponseEntity<Porte> crearPorte(@RequestBody Porte porte) {
        return ResponseEntity.ok(porteService.crearPorte(porte));
    }

    // 2. Ver Ofertas (Conductor)
    @GetMapping("/ofertas/{conductorId}")
    public ResponseEntity<List<Porte>> verOfertas(@PathVariable Long conductorId) {
        return ResponseEntity.ok(porteService.listarOfertasParaConductor(conductorId));
    }

    // 3. Aceptar Porte (Conductor)
    @PostMapping("/{porteId}/aceptar")
    public ResponseEntity<?> aceptarPorte(@PathVariable Long porteId,
                                          @RequestParam Long conductorId) {
        try {
            return ResponseEntity.ok(porteService.aceptarPorte(porteId, conductorId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 4. Cambiar Estado (Entregar, En Tránsito)
    @PutMapping("/{porteId}/estado")
    public ResponseEntity<Porte> cambiarEstado(@PathVariable Long porteId,
                                               @RequestParam EstadoPorte nuevo) {
        return ResponseEntity.ok(porteService.cambiarEstado(porteId, nuevo));
    }

    // 5. Ajuste Manual de Precio (Admin)
    @PostMapping("/{porteId}/ajuste")
    public ResponseEntity<Porte> agregarAjuste(@PathVariable Long porteId,
                                               @RequestParam Double cantidad,
                                               @RequestParam String concepto) {
        return ResponseEntity.ok(porteService.agregarAjusteManual(porteId, cantidad, concepto));
    }

    // 6. Facturar (Admin)
    @PostMapping("/{porteId}/facturar")
    public ResponseEntity<?> generarFactura(@PathVariable Long porteId) {
        try {
            return ResponseEntity.ok(porteService.facturarManualmente(porteId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 7. Obtener Porte por ID (Admin/Cliente/Conductor)
    @GetMapping("/{porteId}")
    public ResponseEntity<?> obtenerPorte(@PathVariable Long porteId) {
        return ResponseEntity.ok(porteService.obtenerPorId(porteId));
    }

    // 8. Listar Portes por Conductor (Conductor)
    @GetMapping("/conductor/{conductorId}")
    public ResponseEntity<List<Porte>> listarPortesConductor(@PathVariable Long conductorId) {
        return ResponseEntity.ok(porteService.listarPortesPorConductor(conductorId));
    }
}
