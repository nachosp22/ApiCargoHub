package com.cargohub.backend.entity;

import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.entity.enums.TipoVehiculo;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify case normalization for key entity fields
 */
public class CaseNormalizationTest {

    @Test
    void testUsuarioEmailNormalizationToLowerCase() {
        // Given
        Usuario usuario = new Usuario();
        
        // When - Set email with mixed case
        usuario.setEmail("Test@Example.COM");
        
        // Then - Email should be normalized to lowercase
        assertEquals("test@example.com", usuario.getEmail());
    }
    
    @Test
    void testUsuarioEmailNullHandling() {
        // Given
        Usuario usuario = new Usuario();
        
        // When - Set email to null
        usuario.setEmail(null);
        
        // Then - Email should remain null
        assertNull(usuario.getEmail());
    }
    
    @Test
    void testVehiculoMatriculaNormalizationToUpperCase() {
        // Given
        Vehiculo vehiculo = new Vehiculo();
        
        // When - Set matricula with mixed case
        vehiculo.setMatricula("abc-1234-def");
        
        // Then - Matricula should be normalized to uppercase
        assertEquals("ABC-1234-DEF", vehiculo.getMatricula());
    }
    
    @Test
    void testVehiculoMatriculaNullHandling() {
        // Given
        Vehiculo vehiculo = new Vehiculo();
        
        // When - Set matricula to null
        vehiculo.setMatricula(null);
        
        // Then - Matricula should remain null
        assertNull(vehiculo.getMatricula());
    }
    
    @Test
    void testConductorDniNormalizationToUpperCase() {
        // Given
        Conductor conductor = new Conductor();
        
        // When - Set DNI with mixed case
        conductor.setDni("12345678a");
        
        // Then - DNI should be normalized to uppercase
        assertEquals("12345678A", conductor.getDni());
    }
    
    @Test
    void testConductorDniNullHandling() {
        // Given
        Conductor conductor = new Conductor();
        
        // When - Set DNI to null
        conductor.setDni(null);
        
        // Then - DNI should remain null
        assertNull(conductor.getDni());
    }
    
    @Test
    void testClienteCifNormalizationToUpperCase() {
        // Given
        Cliente cliente = new Cliente();
        
        // When - Set CIF with mixed case
        cliente.setCif("b12345678");
        
        // Then - CIF should be normalized to uppercase
        assertEquals("B12345678", cliente.getCif());
    }
    
    @Test
    void testClienteCifNullHandling() {
        // Given
        Cliente cliente = new Cliente();
        
        // When - Set CIF to null
        cliente.setCif(null);
        
        // Then - CIF should remain null
        assertNull(cliente.getCif());
    }
    
    @Test
    void testClienteEmailContactoNormalizationToLowerCase() {
        // Given
        Cliente cliente = new Cliente();
        
        // When - Set email with mixed case
        cliente.setEmailContacto("Contact@Company.COM");
        
        // Then - Email should be normalized to lowercase
        assertEquals("contact@company.com", cliente.getEmailContacto());
    }
    
    @Test
    void testClienteEmailContactoNullHandling() {
        // Given
        Cliente cliente = new Cliente();
        
        // When - Set email to null
        cliente.setEmailContacto(null);
        
        // Then - Email should remain null
        assertNull(cliente.getEmailContacto());
    }
}
