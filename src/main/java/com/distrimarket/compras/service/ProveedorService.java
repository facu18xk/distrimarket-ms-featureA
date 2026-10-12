package com.distrimarket.compras.service;

import com.distrimarket.commons.dto.ProveedorRequestDTO;
import com.distrimarket.commons.dto.ProveedorResponseDTO;
import com.distrimarket.commons.service.BaseService;

import java.util.Optional;

public interface ProveedorService extends BaseService<ProveedorRequestDTO, ProveedorResponseDTO, Long> {

    Optional<ProveedorResponseDTO> obtenerPorRuc(String ruc);
}
