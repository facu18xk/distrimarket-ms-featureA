package com.distrimarket.compras.service;

import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.dto.ProveedorRequestDTO;
import com.distrimarket.commons.dto.ProveedorResponseDTO;
import com.distrimarket.commons.service.BaseService;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProveedorService extends BaseService<ProveedorRequestDTO, ProveedorResponseDTO, Long> {

    Optional<ProveedorResponseDTO> obtenerPorRuc(String ruc);

    /**
     * Listado paginado con búsqueda libre y filtro opcional por estado activo/inactivo.
     */
    PageResponseDTO<ProveedorResponseDTO> buscarPaginado(String q, Boolean estado, Pageable pageable);
}
