package com.distrimarket.compras.service;

import com.distrimarket.commons.dto.OrdenCompraDTO;
import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.service.BaseService;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrdenCompraService extends BaseService<OrdenCompraDTO, OrdenCompraDTO, Long> {

    OrdenCompraDTO cambiarEstado(Long id, String nuevoEstado);

    List<OrdenCompraDTO> listarPorProveedor(Long proveedorId);

    /**
     * Listado paginado con búsqueda libre y filtros combinables de proveedor y estado.
     */
    PageResponseDTO<OrdenCompraDTO> buscarPaginado(
            String q, Long proveedorId, String estado, Pageable pageable);
}
