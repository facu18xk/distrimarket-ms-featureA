package com.distrimarket.compras.mapper;

import com.distrimarket.commons.dto.DepositoDTO;
import com.distrimarket.commons.dto.FacturaCompraDetalleRequestDTO;
import com.distrimarket.commons.dto.FacturaCompraDetalleResponseDTO;
import com.distrimarket.commons.dto.FacturaCompraRequestDTO;
import com.distrimarket.commons.dto.FacturaCompraResponseDTO;
import com.distrimarket.commons.dto.MedioPagoDTO;
import com.distrimarket.commons.dto.PersonaDTO;
import com.distrimarket.commons.dto.ProveedorResponseDTO;
import com.distrimarket.commons.entity.Deposito;
import com.distrimarket.commons.entity.FacturaCompra;
import com.distrimarket.commons.entity.FacturaCompraDetalle;
import com.distrimarket.commons.entity.MedioPago;
import com.distrimarket.commons.entity.OrdenCompra;
import com.distrimarket.commons.entity.Persona;
import com.distrimarket.commons.entity.Producto;
import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.commons.mapper.GenericMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FacturaCompraMapper implements GenericMapper<FacturaCompra, FacturaCompraRequestDTO, FacturaCompraResponseDTO> {

    @Override
    public FacturaCompra toEntity(FacturaCompraRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        String condicion = dto.getCondicionCompra() != null ? dto.getCondicionCompra().getValue() : "CONTADO";

        FacturaCompra entity = FacturaCompra.builder()
                .numeroFactura(dto.getNumeroFactura())
                .fechaEmision(dto.getFechaEmision())
                .timbrado(dto.getTimbrado())
                .condicionCompra(condicion)
                .estado("ACTIVA")
                .totalGeneral(BigDecimal.ZERO)
                .totalIva(BigDecimal.ZERO)
                .detalles(new ArrayList<>())
                .build();

        if (dto.getIdProveedor() != null) {
            entity.setProveedor(Proveedor.builder().id(dto.getIdProveedor()).build());
        }
        if (dto.getIdDeposito() != null) {
            entity.setDeposito(Deposito.builder().id(dto.getIdDeposito()).build());
        }
        if (dto.getIdMedioPago() != null) {
            entity.setMedioPago(MedioPago.builder().id(dto.getIdMedioPago()).build());
        }
        if (dto.getIdOrdenCompra() != null) {
            entity.setOrdenCompra(OrdenCompra.builder().id(dto.getIdOrdenCompra()).build());
        }

        if (dto.getDetalles() != null) {
            for (FacturaCompraDetalleRequestDTO dDto : dto.getDetalles()) {
                BigDecimal iva = (dDto.getPorcentajeIva() != null && dDto.getPorcentajeIva().getValue() != null)
                        ? dDto.getPorcentajeIva().getValue()
                        : new BigDecimal("10.00");

                FacturaCompraDetalle dEntity = FacturaCompraDetalle.builder()
                        .producto(dDto.getIdProducto() != null ? Producto.builder().id(dDto.getIdProducto()).build() : null)
                        .cantidad(dDto.getCantidad())
                        .precioUnitario(dDto.getPrecioUnitario() != null ? dDto.getPrecioUnitario() : BigDecimal.ZERO)
                        .porcentajeIva(iva)
                        .build();
                dEntity.calcularSubtotal();
                entity.agregarDetalle(dEntity);
            }
        }

        return entity;
    }

    @Override
    public FacturaCompraResponseDTO toDto(FacturaCompra entity) {
        if (entity == null) {
            return null;
        }

        FacturaCompraResponseDTO dto = new FacturaCompraResponseDTO();
        dto.setIdFacturaCompra(entity.getId());
        dto.setNumeroFactura(entity.getNumeroFactura());
        dto.setFechaEmision(entity.getFechaEmision());
        dto.setTimbrado(entity.getTimbrado());
        dto.setEstado(entity.getEstado());
        dto.setCondicionCompra(entity.getCondicionCompra());
        dto.setTotalGeneral(entity.getTotalGeneral());
        dto.setTotalIva(entity.getTotalIva());

        if (entity.getOrdenCompra() != null) {
            dto.setIdOrdenCompra(entity.getOrdenCompra().getId());
        }

        if (entity.getProveedor() != null) {
            Proveedor prov = entity.getProveedor();
            ProveedorResponseDTO pDto = new ProveedorResponseDTO();
            pDto.setIdProveedor(prov.getId());
            pDto.setNombreFantasia(prov.getNombreFantasia());
            pDto.setEstado(prov.getEstado());

            if (prov.getPersona() != null) {
                Persona per = prov.getPersona();
                PersonaDTO perDto = new PersonaDTO();
                perDto.setIdPersona(per.getId());
                if (per.getTipoPersona() != null) {
                    perDto.setTipoPersona(com.distrimarket.commons.enums.TipoPersona.valueOf(per.getTipoPersona().name()));
                }
                perDto.setNombreCompleto(per.getNombreCompleto());
                perDto.setCi(per.getCi());
                perDto.setRuc(per.getRuc());
                perDto.setTelefono(per.getTelefono());
                perDto.setCorreo(per.getCorreo());
                perDto.setDireccion(per.getDireccion());
                pDto.setPersona(perDto);
            }
            dto.setProveedor(pDto);
        }

        if (entity.getDeposito() != null) {
            Deposito dep = entity.getDeposito();
            DepositoDTO depDto = new DepositoDTO();
            depDto.setIdDeposito(dep.getId());
            depDto.setNombre(dep.getNombre());
            depDto.setUbicacion(dep.getUbicacion());
            depDto.setEstado(dep.getEstado());
            dto.setDeposito(depDto);
        }

        if (entity.getMedioPago() != null) {
            MedioPago mp = entity.getMedioPago();
            MedioPagoDTO mpDto = new MedioPagoDTO();
            mpDto.setIdMedioPago(mp.getId());
            mpDto.setNombre(mp.getNombre());
            mpDto.setActivo(mp.getActivo());
            dto.setMedioPago(mpDto);
        }

        if (entity.getDetalles() != null) {
            List<FacturaCompraDetalleResponseDTO> detallesDto = entity.getDetalles().stream()
                    .map(d -> {
                        FacturaCompraDetalleResponseDTO dDto = new FacturaCompraDetalleResponseDTO();
                        dDto.setIdDetalle(d.getId());
                        if (d.getProducto() != null) {
                            dDto.setIdProducto(d.getProducto().getId());
                            dDto.setNombreProducto(d.getProducto().getNombre());
                            dDto.setCodigoBarra(d.getProducto().getCodigoBarra());
                        }
                        dDto.setCantidad(d.getCantidad());
                        dDto.setPrecioUnitario(d.getPrecioUnitario());
                        dDto.setSubtotal(d.getSubtotal());
                        dDto.setPorcentajeIva(d.getPorcentajeIva());
                        dDto.setMontoIva(d.getMontoIva());
                        return dDto;
                    })
                    .collect(Collectors.toList());
            dto.setDetalles(detallesDto);
        }

        return dto;
    }

    @Override
    public void updateEntity(FacturaCompra entity, FacturaCompraRequestDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        if (dto.getNumeroFactura() != null) {
            entity.setNumeroFactura(dto.getNumeroFactura());
        }
        if (dto.getFechaEmision() != null) {
            entity.setFechaEmision(dto.getFechaEmision());
        }
        if (dto.getTimbrado() != null) {
            entity.setTimbrado(dto.getTimbrado());
        }
        if (dto.getCondicionCompra() != null) {
            entity.setCondicionCompra(dto.getCondicionCompra().getValue());
        }
    }
}
