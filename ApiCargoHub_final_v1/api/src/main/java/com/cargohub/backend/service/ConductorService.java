package com.cargohub.backend.service;

import com.cargohub.backend.entity.BloqueoAgenda;
import com.cargohub.backend.entity.Conductor;
import com.cargohub.backend.entity.Usuario;
import com.cargohub.backend.entity.Vehiculo;
import com.cargohub.backend.entity.enums.EstadoVehiculo;
import com.cargohub.backend.entity.enums.RolUsuario;
import com.cargohub.backend.repository.BloqueoAgendaRepository;
import com.cargohub.backend.repository.ConductorRepository;
import com.cargohub.backend.repository.UsuarioRepository;
import com.cargohub.backend.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConductorService {

    @Autowired private ConductorRepository conductorRepository;
    @Autowired private BloqueoAgendaRepository bloqueoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private VehiculoRepository vehiculoRepository; // Para dar de baja sus camiones
    @Autowired private UsuarioService usuarioService;

    // --- 1. GESTIÓN DE PERFIL ---

    /**
     * Guarda un nuevo conductor o actualiza uno existente en la base de datos.
     *
     * @param conductor la entidad {@link Conductor} a persistir
     * @return el conductor guardado con los datos actualizados
     */
    @Transactional
    public Conductor guardarOActualizar(Conductor conductor) {
        return conductorRepository.save(conductor);
    }

    /**
     * Obtiene un conductor por su identificador único.
     *
     * @param id el identificador del conductor
     * @return la entidad {@link Conductor} encontrada
     * @throws RuntimeException si no se encuentra ningún conductor con el ID indicado
     */
    public Conductor obtenerPorId(Long id) {
        return conductorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));
    }

    /**
     * Busca un conductor a partir del email de su usuario asociado.
     * El email se normaliza a minúsculas antes de la búsqueda.
     *
     * @param email el correo electrónico del usuario vinculado al conductor
     * @return la entidad {@link Conductor} asociada al email
     * @throws RuntimeException si no existe un conductor asociado a ese email
     */
    public Conductor obtenerPorEmailUsuario(String email) {
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        return conductorRepository.findByUsuarioEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("No existe conductor asociado a este email"));
    }

    /**
     * Devuelve la lista completa de conductores registrados en el sistema.
     *
     * @return lista de todos los {@link Conductor} existentes
     */
    public List<Conductor> listarTodos() {
        return conductorRepository.findAll();
    }

    /**
     * Crea un nuevo conductor junto con su usuario asociado con rol de conductor.
     * El usuario se marca como activo y el conductor queda disponible por defecto.
     *
     * @param nombre       nombre del conductor
     * @param apellidos    apellidos del conductor
     * @param email        correo electrónico para el usuario
     * @param password     contraseña para el usuario
     * @param dni          documento nacional de identidad
     * @param telefono     número de teléfono de contacto
     * @param ciudadBase   ciudad base de operaciones del conductor
     * @return el {@link Conductor} recién creado y persistido
     */
    @Transactional
    public Conductor crearConductorAdmin(String nombre,
                                         String apellidos,
                                         String email,
                                         String password,
                                         String dni,
                                         String telefono,
                                         String ciudadBase) {
        Usuario usuario = usuarioService.registrarUsuario(email, password, RolUsuario.CONDUCTOR);
        usuario.setActivo(true);
        usuario = usuarioService.guardar(usuario);

        Conductor conductor = new Conductor();
        conductor.setUsuario(usuario);
        conductor.setNombre(nombre);
        conductor.setApellidos(apellidos);
        conductor.setDni(dni);
        conductor.setTelefono(telefono);
        conductor.setCiudadBase(ciudadBase);
        conductor.setDisponible(true);

        return conductorRepository.save(conductor);
    }

    // --- APROBACIÓN DE CONDUCTORES ---

    /**
     * Obtiene la lista de conductores que aún no han sido aprobados por un administrador.
     *
     * @return lista de conductores pendientes de aprobación
     */
    public List<Conductor> listarPendientesAprobacion() {
        return conductorRepository.findPendientesAprobacion();
    }

    /**
     * Aprueba un conductor activando su usuario asociado para que pueda iniciar sesión.
     *
     * @param conductorId el identificador del conductor a aprobar
     * @return el {@link Conductor} aprobado
     * @throws RuntimeException si el conductor no tiene usuario asociado o ya está aprobado
     */
    @Transactional
    public Conductor aprobarConductor(Long conductorId) {
        Conductor conductor = obtenerPorId(conductorId);
        Usuario usuario = conductor.getUsuario();
        if (usuario == null) {
            throw new RuntimeException("Conductor sin usuario asociado");
        }
        if (usuario.isActivo()) {
            throw new RuntimeException("El conductor ya está aprobado");
        }
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        return conductor;
    }

    /**
     * Rechaza un conductor eliminando tanto la entidad conductor como su usuario asociado.
     *
     * @param conductorId el identificador del conductor a rechazar
     */
    @Transactional
    public void rechazarConductor(Long conductorId) {
        Conductor conductor = obtenerPorId(conductorId);
        Usuario usuario = conductor.getUsuario();
        conductorRepository.delete(conductor);
        if (usuario != null) {
            usuarioRepository.delete(usuario);
        }
    }

    // --- NUEVO: DAR DE BAJA (SOFT DELETE) ---

    /**
     * Da de baja un conductor de forma lógica: desactiva su disponibilidad,
     * bloquea el acceso de su usuario y marca como baja todos sus vehículos asociados.
     * El historial del conductor se conserva en la base de datos.
     *
     * @param conductorId el identificador del conductor a dar de baja
     */
    @Transactional
    public void darDeBajaConductor(Long conductorId) {
        Conductor conductor = obtenerPorId(conductorId);

        // 1. Desactivar disponibilidad del Conductor
        conductor.setDisponible(false);
        conductorRepository.save(conductor);

        // 2. Bloquear acceso al Usuario (Login)
        Usuario usuario = conductor.getUsuario();
        if (usuario != null) {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
        }

        // 3. (Opcional) Dar de baja sus vehículos asociados
        List<Vehiculo> flota = vehiculoRepository.findByConductorId(conductorId);
        for (Vehiculo v : flota) {
            v.setEstado(EstadoVehiculo.BAJA);
            vehiculoRepository.save(v);
        }

        System.out.println("Conductor ID " + conductorId + " dado de baja correctamente (Historial conservado).");
    }

    // --- 2. OPERATIVA DIARIA ---

    /**
     * Actualiza la ubicación de un conductor con los datos mínimos de latitud y longitud.
     * Delega en la versión completa pasando {@code null} para los campos opcionales.
     *
     * @param conductorId el identificador del conductor
     * @param lat         latitud actual
     * @param lon         longitud actual
     */
    @Transactional
    public void actualizarUbicacion(Long conductorId, Double lat, Double lon) {
        actualizarUbicacion(conductorId, lat, lon, null, null, null);
    }

    /**
     * Actualiza la ubicación de un conductor con todos los datos disponibles.
     * Si el conductor está dado de baja (usuario inactivo), se ignora la actualización.
     *
     * @param conductorId el identificador del conductor
     * @param lat         latitud actual
     * @param lon         longitud actual
     * @param recordedAt  fecha y hora del registro (si es {@code null}, se usa la hora actual)
     * @param speedKph    velocidad en kilómetros por hora
     * @param headingDeg  rumbo en grados
     */
    @Transactional
    public void actualizarUbicacion(Long conductorId,
                                    Double lat,
                                    Double lon,
                                    LocalDateTime recordedAt,
                                    Double speedKph,
                                    Integer headingDeg) {
        Conductor c = obtenerPorId(conductorId);
        // Si está dado de baja, no debería poder reportar ubicación
        if (c.getUsuario() != null && !c.getUsuario().isActivo()) return;

        c.setLatitudActual(lat);
        c.setLongitudActual(lon);
        c.setUltimaActualizacionUbicacion(recordedAt != null ? recordedAt : LocalDateTime.now());
        c.setVelocidadKphActual(speedKph);
        c.setRumboActualDeg(headingDeg);
        c.setDisponible(true);
        conductorRepository.save(c);
    }

    /**
     * Cambia el estado de disponibilidad de un conductor.
     *
     * @param conductorId el identificador del conductor
     * @param disponible  {@code true} para marcarlo como disponible, {@code false} en caso contrario
     */
    public void cambiarDisponibilidad(Long conductorId, boolean disponible) {
        Conductor c = obtenerPorId(conductorId);
        c.setDisponible(disponible);
        conductorRepository.save(c);
    }

    // --- 3. AGENDA ---

    /**
     * Obtiene los bloqueos de agenda de un conductor dentro de un rango de fechas.
     *
     * @param conductorId el identificador del conductor
     * @param desde       fecha y hora de inicio del rango
     * @param hasta       fecha y hora de fin del rango
     * @return lista de {@link BloqueoAgenda} dentro del período indicado
     */
    public List<BloqueoAgenda> obtenerAgenda(Long conductorId, LocalDateTime desde, LocalDateTime hasta) {
        return bloqueoRepository.findByConductorIdAndFechaInicioBetween(conductorId, desde, hasta);
    }

    /**
     * Agrega un nuevo bloqueo a la agenda de un conductor.
     *
     * @param conductorId el identificador del conductor
     * @param bloqueo     el bloqueo de agenda a registrar
     * @return el {@link BloqueoAgenda} persistido con su identificador asignado
     */
    @Transactional
    public BloqueoAgenda agregarBloqueo(Long conductorId, BloqueoAgenda bloqueo) {
        Conductor c = obtenerPorId(conductorId);
        bloqueo.setConductor(c);
        return bloqueoRepository.save(bloqueo);
    }

    /**
     * Elimina un bloqueo de agenda por su identificador.
     *
     * @param bloqueoId el identificador del bloqueo a eliminar
     */
    @Transactional
    public void eliminarBloqueo(Long bloqueoId) {
        bloqueoRepository.deleteById(bloqueoId);
    }

    /**
     * Obtiene los días laborables configurados para un conductor como lista de enteros.
     *
     * @param conductorId el identificador del conductor
     * @return lista de días laborables (1=lunes, 7=domingo)
     */
    public List<Integer> obtenerDiasLaborables(Long conductorId) {
        Conductor conductor = obtenerPorId(conductorId);
        return parseDiasLaborables(conductor.getDiasLaborables());
    }

    /**
     * Actualiza los días laborables de un conductor y devuelve la lista resultante parseada.
     *
     * @param conductorId   el identificador del conductor
     * @param diasLaborables lista de días laborables a establecer (1=lunes, 7=domingo)
     * @return lista de días laborables después de la actualización
     */
    @Transactional
    public List<Integer> actualizarDiasLaborables(Long conductorId, List<Integer> diasLaborables) {
        Conductor conductor = obtenerPorId(conductorId);
        conductor.setDiasLaborables(formatDiasLaborables(diasLaborables));
        conductorRepository.save(conductor);
        return parseDiasLaborables(conductor.getDiasLaborables());
    }

    /**
     * Convierte una cadena de días laborables separados por comas en una lista de enteros.
     * Los valores inválidos se ignoran silenciosamente y se filtran duplicados.
     *
     * @param diasLaborablesRaw la cadena con los días separados por comas
     * @return lista de enteros válidos entre 1 y 7 sin duplicados
     */
    private List<Integer> parseDiasLaborables(String diasLaborablesRaw) {
        List<Integer> resultado = new ArrayList<>();
        if (diasLaborablesRaw == null || diasLaborablesRaw.trim().isEmpty()) {
            return resultado;
        }
        String[] tokens = diasLaborablesRaw.split(",");
        for (String token : tokens) {
            try {
                int dia = Integer.parseInt(token.trim());
                if (dia >= 1 && dia <= 7 && !resultado.contains(dia)) {
                    resultado.add(dia);
                }
            } catch (NumberFormatException ignored) {
                // Ignora tokens inválidos
            }
        }
        return resultado;
    }

    /**
     * Convierte una lista de días laborables en una cadena separada por comas.
     * Los días se normalizan al rango 1-7, se eliminan duplicados y se ordenan ascendentemente.
     *
     * @param diasLaborables la lista de días laborables
     * @return cadena con los días separados por comas, o cadena vacía si la lista es nula o vacía
     */
    private String formatDiasLaborables(List<Integer> diasLaborables) {
        if (diasLaborables == null || diasLaborables.isEmpty()) {
            return "";
        }
        List<Integer> normalizados = new ArrayList<>();
        for (Integer dia : diasLaborables) {
            if (dia != null && dia >= 1 && dia <= 7 && !normalizados.contains(dia)) {
                normalizados.add(dia);
            }
        }
        normalizados.sort(Integer::compareTo);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < normalizados.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(normalizados.get(i));
        }
        return sb.toString();
    }
}
