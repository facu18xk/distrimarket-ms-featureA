package com.distrimarket.compras.service.impl;

import com.distrimarket.commons.dto.PersonaRequestDTO;
import com.distrimarket.commons.dto.ProveedorRequestDTO;
import com.distrimarket.commons.dto.ProveedorResponseDTO;
import com.distrimarket.commons.entity.Persona;
import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.commons.service.impl.BaseServiceImpl;
import com.distrimarket.compras.exception.BadRequestException;
import com.distrimarket.compras.exception.ConflictException;
import com.distrimarket.compras.mapper.ProveedorMapper;
import com.distrimarket.compras.repository.PersonaRepository;
import com.distrimarket.compras.repository.ProveedorRepository;
import com.distrimarket.compras.service.ProveedorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional
public class ProveedorServiceImpl extends BaseServiceImpl<Proveedor, Long, ProveedorRequestDTO, ProveedorResponseDTO> implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final PersonaRepository personaRepository;

    public ProveedorServiceImpl(
            ProveedorRepository proveedorRepository,
            ProveedorMapper proveedorMapper,
            PersonaRepository personaRepository) {
        super(proveedorRepository, proveedorMapper);
        this.proveedorRepository = proveedorRepository;
        this.personaRepository = personaRepository;
    }

    @Override
    public ProveedorResponseDTO crear(ProveedorRequestDTO dto) {
        log.info("Registrando proveedor comercial: {}", dto.getNombreFantasia());

        if (dto.getPersona() == null) {
            throw new BadRequestException("Los datos de la persona o razón social son obligatorios para crear un proveedor");
        }

        PersonaRequestDTO pDto = dto.getPersona();
        Persona persona = null;

        if (pDto.getRuc() != null && !pDto.getRuc().isBlank()) {
            persona = personaRepository.findByRuc(pDto.getRuc()).orElse(null);
        } else if (pDto.getCi() != null && !pDto.getCi().isBlank()) {
            persona = personaRepository.findByCi(pDto.getCi()).orElse(null);
        }

        if (persona == null) {
            com.distrimarket.commons.enums.TipoPersona tipo = pDto.getTipoPersona() != null
                    ? com.distrimarket.commons.enums.TipoPersona.valueOf(pDto.getTipoPersona().name())
                    : null;
            persona = Persona.builder()
                    .tipoPersona(tipo)
                    .nombreCompleto(pDto.getNombreCompleto())
                    .ci(pDto.getCi())
                    .ruc(pDto.getRuc())
                    .telefono(pDto.getTelefono())
                    .correo(pDto.getCorreo())
                    .direccion(pDto.getDireccion())
                    .build();
            persona = personaRepository.save(persona);
        } else {
            // Verificar si esta persona ya está asignada a un proveedor
            if (proveedorRepository.findByPersonaId(persona.getId()).isPresent()) {
                throw new ConflictException("Ya existe un proveedor registrado para la persona / RUC indicado");
            }
        }

        Proveedor proveedor = Proveedor.builder()
                .persona(persona)
                .nombreFantasia(dto.getNombreFantasia())
                .estado(dto.getEstado() != null ? dto.getEstado() : true)
                .build();

        Proveedor guardado = proveedorRepository.save(proveedor);
        log.info("Proveedor registrado exitosamente con ID: {}", guardado.getId());

        return mapper.toDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProveedorResponseDTO> obtenerPorRuc(String ruc) {
        return proveedorRepository.findByPersonaRuc(ruc).map(mapper::toDto);
    }
}
