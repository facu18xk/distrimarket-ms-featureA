package com.distrimarket.compras.mapper;

import com.distrimarket.commons.dto.OrdenCompraDTO;
import com.distrimarket.commons.dto.OrdenCompraDetalleDTO;
import com.distrimarket.commons.entity.Empleado;
import com.distrimarket.commons.entity.OrdenCompra;
import com.distrimarket.commons.entity.OrdenCompraDetalle;
import com.distrimarket.commons.entity.Producto;
import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.commons.mapper.GenericMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrdenCompraMapper implements GenericMapper<OrdenCompra, OrdenCompraDTO, OrdenCompraDTO> {

    @Override
    public OrdenCompra toEntity(OrdenCompraDTO dto) {
        if (dto == null) {
            return null;
        }

        OrdenCompra entity = OrdenCompra.builder()
                .id(dto.getId())
                .estado(dto.getEstado() != null ? dto.getEstado() : "PENDIENTE")
                .observacion(dto.getObservacion())
                .total(dto.getTotal() != null ? BigDecimal.valueOf(dto.getTotal()) : BigDecimal.ZERO)
                .detalles(new ArrayList<>())
                .build();

        if (dto.getIdProveedor() != null) {
            entity.setProveedor(Proveedor.builder().id(dto.getIdProveedor()).build());
        }

        if (dto.getIdEmpleado() != null) {
            entity.setEmpleado(Empleado.builder().id(dto.getIdEmpleado()).build());
        }

        if (dto.getDetalles() != null) {
            for (OrdenCompraDetalleDTO dDto : dto.getDetalles()) {
                OrdenCompraDetalle dEntity = OrdenCompraDetalle.builder()
                        .id(dDto.getId())
                        .producto(dDto.getIdProducto() != null ? Producto.builder().id(dDto.getIdProducto()).build() : null)
                        .cantidad(dDto.getCantidad())
                        .precioUnitario(dDto.getPrecioUnitario() != null ? BigDecimal.valueOf(dDto.getPrecioUnitario()) : BigDecimal.ZERO)
                        .subtotal(dDto.getSubtotal() != null ? BigDecimal.valueOf(dDto.getSubtotal()) : BigDecimal.ZERO)
                        .build();
                entity.agregarDetalle(dEntity);
            }
        }

        return entity;
    }

    @Override
    public OrdenCompraDTO toDto(OrdenCompra entity) {
        if (entity == null) {
            return null;
        }

        OrdenCompraDTO dto = new OrdenCompraDTO();
        dto.setId(entity.getId());
        dto.setIdProveedor(entity.getProveedor() != null ? entity.getProveedor().getId() : null);
        dto.setIdEmpleado(entity.getEmpleado() != null ? entity.getEmpleado().getId() : null);
        dto.setEstado(entity.getEstado());
        dto.setObservacion(entity.getObservacion());
        dto.setTotal(entity.getTotal() != null ? entity.getTotal().doubleValue() : 0.0);

        if (entity.getDetalles() != null) {
            List<OrdenCompraDetalleDTO> detallesDto = entity.getDetalles().stream()
                    .map(d -> {
                        OrdenCompraDetalleDTO dDto = new OrdenCompraDetalleDTO();
                        dDto.setId(d.getId());
                        dDto.setIdProducto(d.getProducto() != null ? d.getProducto().getId() : null);
                        dDto.setCantidad(d.getCantidad());
                        dDto.setPrecioUnitario(d.getPrecioUnitario() != null ? d.getPrecioUnitario().doubleValue() : 0.0);
                        dDto.setSubtotal(d.getSubtotal() != null ? d.getSubtotal().doubleValue() : 0.0);
                        return dDto;
                    })
                    .collect(Collectors.toList());
            dto.setDetalles(detallesDto);
        }

        return dto;
    }

    @Override
    public void updateEntity(OrdenCompra entity, OrdenCompraDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        if (dto.getEstado() != null) {
            entity.setEstado(dto.getEstado());
        }
        if (dto.getObservacion() != null) {
            entity.setObservacion(dto.getObservacion());
        }
        if (dto.getTotal() != null) {
            entity.setTotal(BigDecimal.valueOf(dto.getTotal()));
        }
    }
}
