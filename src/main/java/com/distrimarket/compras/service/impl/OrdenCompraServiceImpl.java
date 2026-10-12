package com.distrimarket.compras.service.impl;

import com.distrimarket.commons.dto.OrdenCompraDTO;
import com.distrimarket.commons.dto.OrdenCompraDetalleDTO;
import com.distrimarket.commons.entity.Empleado;
import com.distrimarket.commons.entity.OrdenCompra;
import com.distrimarket.commons.entity.OrdenCompraDetalle;
import com.distrimarket.commons.entity.Producto;
import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.commons.exception.ResourceNotFoundException;
import com.distrimarket.commons.service.impl.BaseServiceImpl;
import com.distrimarket.compras.mapper.OrdenCompraMapper;
import com.distrimarket.compras.repository.EmpleadoRepository;
import com.distrimarket.compras.repository.OrdenCompraRepository;
import com.distrimarket.compras.repository.ProductoRepository;
import com.distrimarket.compras.repository.ProveedorRepository;
import com.distrimarket.compras.service.OrdenCompraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class OrdenCompraServiceImpl extends BaseServiceImpl<OrdenCompra, Long, OrdenCompraDTO, OrdenCompraDTO> implements OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ProductoRepository productoRepository;

    public OrdenCompraServiceImpl(
            OrdenCompraRepository ordenCompraRepository,
            OrdenCompraMapper ordenCompraMapper,
            ProveedorRepository proveedorRepository,
            EmpleadoRepository empleadoRepository,
            ProductoRepository productoRepository) {
        super(ordenCompraRepository, ordenCompraMapper);
        this.ordenCompraRepository = ordenCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.empleadoRepository = empleadoRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public OrdenCompraDTO crear(OrdenCompraDTO dto) {
        log.info("Creando orden de compra para proveedor ID: {}", dto.getIdProveedor());

        Proveedor proveedor = proveedorRepository.findById(dto.getIdProveedor())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", "id", dto.getIdProveedor()));

        Empleado empleado = empleadoRepository.findById(dto.getIdEmpleado())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", dto.getIdEmpleado()));

        OrdenCompra ordenCompra = OrdenCompra.builder()
                .proveedor(proveedor)
                .empleado(empleado)
                .estado(dto.getEstado() != null ? dto.getEstado() : "PENDIENTE")
                .observacion(dto.getObservacion())
                .total(BigDecimal.ZERO)
                .detalles(new ArrayList<>())
                .build();

        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
            for (OrdenCompraDetalleDTO dDto : dto.getDetalles()) {
                Producto producto = productoRepository.findById(dDto.getIdProducto())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", dDto.getIdProducto()));

                BigDecimal precioUnitario = dDto.getPrecioUnitario() != null
                        ? BigDecimal.valueOf(dDto.getPrecioUnitario())
                        : producto.getPrecioCosto();

                OrdenCompraDetalle detalle = OrdenCompraDetalle.builder()
                        .producto(producto)
                        .cantidad(dDto.getCantidad())
                        .precioUnitario(precioUnitario)
                        .build();

                detalle.calcularSubtotal();
                ordenCompra.agregarDetalle(detalle);
            }
        }

        ordenCompra.recalcularTotal();
        OrdenCompra guardada = ordenCompraRepository.save(ordenCompra);
        log.info("Orden de compra creada con ID: {} y total: {}", guardada.getId(), guardada.getTotal());
        return mapper.toDto(guardada);
    }

    @Override
    public OrdenCompraDTO cambiarEstado(Long id, String nuevoEstado) {
        log.info("Cambiando estado de orden de compra ID: {} a {}", id, nuevoEstado);
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrdenCompra", "id", id));
        orden.setEstado(nuevoEstado);
        OrdenCompra actualizada = ordenCompraRepository.save(orden);
        return mapper.toDto(actualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenCompraDTO> listarPorProveedor(Long proveedorId) {
        return ordenCompraRepository.findByProveedorId(proveedorId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
