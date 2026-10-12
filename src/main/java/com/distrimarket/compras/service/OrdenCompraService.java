package com.distrimarket.compras.service;

import com.distrimarket.commons.dto.OrdenCompraDTO;
import com.distrimarket.commons.service.BaseService;

import java.util.List;

public interface OrdenCompraService extends BaseService<OrdenCompraDTO, OrdenCompraDTO, Long> {

    OrdenCompraDTO cambiarEstado(Long id, String nuevoEstado);

    List<OrdenCompraDTO> listarPorProveedor(Long proveedorId);
}
