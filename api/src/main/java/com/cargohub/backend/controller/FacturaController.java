package com.cargohub.backend.controller;

import com.cargohub.backend.entity.Factura;
import com.cargohub.backend.repository.FacturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaController {

    @Autowired
    private FacturaRepository facturaRepository;

    // 1. Listar todas las facturas (Panel de Admin)
    @GetMapping
    public ResponseEntity<List<Factura>> listarTodas() {
        return ResponseEntity.ok(facturaRepository.findAll());
    }

    // 2. Ver una factura concreta por ID
    @GetMapping("/{id}")
    public ResponseEntity<Factura> verDetalle(@PathVariable Long id) {
        return facturaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Obtener factura por ID de Porte (útil para el cliente)
    @GetMapping("/porte/{porteId}")
    public ResponseEntity<Factura> buscarPorPorte(@PathVariable Long porteId) {
        return facturaRepository.findByPorteId(porteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
