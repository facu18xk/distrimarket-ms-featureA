package com.distrimarket.compras.mapper;

import com.distrimarket.commons.dto.PersonaCreateDTO;
import com.distrimarket.commons.dto.PersonaDTO;
import com.distrimarket.commons.dto.ProveedorRequestDTO;
import com.distrimarket.commons.dto.ProveedorResponseDTO;
import com.distrimarket.commons.entity.Persona;
import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.commons.mapper.GenericMapper;
import org.springframework.stereotype.Component;

@Component
public class ProveedorMapper implements GenericMapper<Proveedor, ProveedorRequestDTO, ProveedorResponseDTO> {

    @Override
    public Proveedor toEntity(ProveedorRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Proveedor entity = Proveedor.builder()
                .nombreFantasia(dto.getNombreFantasia())
                .estado(dto.getEstado() != null ? dto.getEstado() : true)
                .build();

        if (dto.getPersona() != null) {
            PersonaCreateDTO pDto = dto.getPersona();
            com.distrimarket.commons.enums.TipoPersona tipo = pDto.getTipoPersona() != null
                    ? com.distrimarket.commons.enums.TipoPersona.valueOf(pDto.getTipoPersona().name())
                    : null;
            Persona persona = Persona.builder()
                    .tipoPersona(tipo)
                    .nombreCompleto(pDto.getNombreCompleto())
                    .ci(pDto.getCi())
                    .ruc(pDto.getRuc())
                    .telefono(pDto.getTelefono())
                    .correo(pDto.getCorreo())
                    .direccion(pDto.getDireccion())
                    .build();
            entity.setPersona(persona);
        }

        return entity;
    }

    @Override
    public ProveedorResponseDTO toDto(Proveedor entity) {
        if (entity == null) {
            return null;
        }

        ProveedorResponseDTO dto = new ProveedorResponseDTO();
        dto.setIdProveedor(entity.getId());
        dto.setNombreFantasia(entity.getNombreFantasia());
        dto.setEstado(entity.getEstado());

        if (entity.getPersona() != null) {
            Persona p = entity.getPersona();
            PersonaDTO pDto = new PersonaDTO();
            pDto.setIdPersona(p.getId());
            if (p.getTipoPersona() != null) {
                pDto.setTipoPersona(com.distrimarket.commons.enums.TipoPersona.valueOf(p.getTipoPersona().name()));
            }
            pDto.setNombreCompleto(p.getNombreCompleto());
            pDto.setCi(p.getCi());
            pDto.setRuc(p.getRuc());
            pDto.setTelefono(p.getTelefono());
            pDto.setCorreo(p.getCorreo());
            pDto.setDireccion(p.getDireccion());
            dto.setPersona(pDto);
        }

        return dto;
    }

    @Override
    public void updateEntity(Proveedor entity, ProveedorRequestDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        if (dto.getNombreFantasia() != null) {
            entity.setNombreFantasia(dto.getNombreFantasia());
        }
        if (dto.getEstado() != null) {
            entity.setEstado(dto.getEstado());
        }
        if (dto.getPersona() != null && entity.getPersona() != null) {
            PersonaCreateDTO pDto = dto.getPersona();
            Persona p = entity.getPersona();
            if (pDto.getTipoPersona() != null) {
                p.setTipoPersona(com.distrimarket.commons.enums.TipoPersona.valueOf(pDto.getTipoPersona().name()));
            }
            if (pDto.getNombreCompleto() != null) p.setNombreCompleto(pDto.getNombreCompleto());
            if (pDto.getCi() != null) p.setCi(pDto.getCi());
            if (pDto.getRuc() != null) p.setRuc(pDto.getRuc());
            if (pDto.getTelefono() != null) p.setTelefono(pDto.getTelefono());
            if (pDto.getCorreo() != null) p.setCorreo(pDto.getCorreo());
            if (pDto.getDireccion() != null) p.setDireccion(pDto.getDireccion());
        }
    }
}
