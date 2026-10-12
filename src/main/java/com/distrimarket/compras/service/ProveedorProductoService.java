package com.distrimarket.compras.service;

import com.distrimarket.commons.dto.ProveedorProductoDTO;
import com.distrimarket.commons.service.BaseService;

import java.util.List;
import java.util.Optional;

public interface ProveedorProductoService extends BaseService<ProveedorProductoDTO, ProveedorProductoDTO, Long> {

    List<ProveedorProductoDTO> listarPorProveedor(Long proveedorId);

    Optional<ProveedorProductoDTO> obtenerPorProveedorYProducto(Long proveedorId, Long productoId);
}
