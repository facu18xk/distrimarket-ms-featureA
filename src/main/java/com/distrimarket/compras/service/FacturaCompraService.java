package com.distrimarket.compras.service;

import com.distrimarket.commons.dto.FacturaCompraRequestDTO;
import com.distrimarket.commons.dto.FacturaCompraResponseDTO;
import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.service.BaseService;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface FacturaCompraService extends BaseService<FacturaCompraRequestDTO, FacturaCompraResponseDTO, Long> {

    List<FacturaCompraResponseDTO> listarPorProveedor(Long proveedorId);

    FacturaCompraResponseDTO anularFactura(Long id);

    /**
     * Búsqueda paginada con filtros combinables de facturas de compra.
     *
     * @param proveedorId   filtra por proveedor (opcional)
     * @param estado        filtra por estado exacto, ej. ACTIVA / ANULADA (opcional)
     * @param desde         fecha de emisión mínima inclusive (opcional)
     * @param hasta         fecha de emisión máxima inclusive (opcional)
     * @param numeroFactura coincide parcialmente con el número de factura (opcional)
     * @param pageable      paginación y ordenamiento obligatorios
     */
    PageResponseDTO<FacturaCompraResponseDTO> buscarPaginado(
            Long proveedorId, String estado, LocalDate desde, LocalDate hasta,
            String numeroFactura, Pageable pageable);
}
