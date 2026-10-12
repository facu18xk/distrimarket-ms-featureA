package com.distrimarket.compras.service;

import com.distrimarket.commons.dto.FacturaCompraRequestDTO;
import com.distrimarket.commons.dto.FacturaCompraResponseDTO;
import com.distrimarket.commons.service.BaseService;

import java.util.List;

public interface FacturaCompraService extends BaseService<FacturaCompraRequestDTO, FacturaCompraResponseDTO, Long> {

    List<FacturaCompraResponseDTO> listarPorProveedor(Long proveedorId);

    FacturaCompraResponseDTO anularFactura(Long id);
}
