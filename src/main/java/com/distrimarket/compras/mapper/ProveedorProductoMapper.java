package com.distrimarket.compras.mapper;

import com.distrimarket.commons.dto.ProveedorProductoDTO;
import com.distrimarket.commons.entity.Producto;
import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.commons.entity.ProveedorProducto;
import com.distrimarket.commons.mapper.GenericMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProveedorProductoMapper implements GenericMapper<ProveedorProducto, ProveedorProductoDTO, ProveedorProductoDTO> {

    @Override
    public ProveedorProducto toEntity(ProveedorProductoDTO dto) {
        if (dto == null) {
            return null;
        }

        ProveedorProducto entity = ProveedorProducto.builder()
                .id(dto.getId())
                .precioCostoAcordado(dto.getPrecioCostoAcordado() != null ? BigDecimal.valueOf(dto.getPrecioCostoAcordado()) : BigDecimal.ZERO)
                .codigoProveedor(dto.getCodigoProveedor())
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .build();

        if (dto.getIdProveedor() != null) {
            entity.setProveedor(Proveedor.builder().id(dto.getIdProveedor()).build());
        }
        if (dto.getIdProducto() != null) {
            entity.setProducto(Producto.builder().id(dto.getIdProducto()).build());
        }

        return entity;
    }

    @Override
    public ProveedorProductoDTO toDto(ProveedorProducto entity) {
        if (entity == null) {
            return null;
        }

        ProveedorProductoDTO dto = new ProveedorProductoDTO();
        dto.setId(entity.getId());
        dto.setIdProveedor(entity.getProveedor() != null ? entity.getProveedor().getId() : null);
        dto.setIdProducto(entity.getProducto() != null ? entity.getProducto().getId() : null);
        dto.setPrecioCostoAcordado(entity.getPrecioCostoAcordado() != null ? entity.getPrecioCostoAcordado().doubleValue() : 0.0);
        dto.setCodigoProveedor(entity.getCodigoProveedor());
        dto.setActivo(entity.getActivo());
        return dto;
    }

    @Override
    public void updateEntity(ProveedorProducto entity, ProveedorProductoDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        if (dto.getPrecioCostoAcordado() != null) {
            entity.setPrecioCostoAcordado(BigDecimal.valueOf(dto.getPrecioCostoAcordado()));
        }
        if (dto.getCodigoProveedor() != null) {
            entity.setCodigoProveedor(dto.getCodigoProveedor());
        }
        if (dto.getActivo() != null) {
            entity.setActivo(dto.getActivo());
        }
    }
}
