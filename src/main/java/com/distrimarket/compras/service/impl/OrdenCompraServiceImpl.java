package com.distrimarket.compras.service.impl;

import com.distrimarket.commons.dto.OrdenCompraDTO;
import com.distrimarket.commons.dto.OrdenCompraDetalleDTO;
import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.entity.Empleado;
import com.distrimarket.commons.entity.OrdenCompra;
import com.distrimarket.commons.entity.OrdenCompraDetalle;
import com.distrimarket.commons.entity.Producto;
import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.commons.exception.ResourceNotFoundException;
import com.distrimarket.commons.service.impl.BaseServiceImpl;
import com.distrimarket.compras.config.cache.CacheDomains;
import com.distrimarket.compras.config.cache.DomainCache;
import com.distrimarket.compras.mapper.OrdenCompraMapper;
import com.distrimarket.compras.repository.EmpleadoRepository;
import com.distrimarket.compras.repository.OrdenCompraRepository;
import com.distrimarket.compras.repository.ProductoRepository;
import com.distrimarket.compras.repository.ProveedorRepository;
import com.distrimarket.compras.service.OrdenCompraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class OrdenCompraServiceImpl extends BaseServiceImpl<OrdenCompra, Long, OrdenCompraDTO, OrdenCompraDTO> implements OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ProductoRepository productoRepository;
    private final DomainCache domainCache;

    public OrdenCompraServiceImpl(
            OrdenCompraRepository ordenCompraRepository,
            OrdenCompraMapper ordenCompraMapper,
            ProveedorRepository proveedorRepository,
            EmpleadoRepository empleadoRepository,
            ProductoRepository productoRepository,
            DomainCache domainCache) {
        super(ordenCompraRepository, ordenCompraMapper);
        this.ordenCompraRepository = ordenCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.empleadoRepository = empleadoRepository;
        this.productoRepository = productoRepository;
        this.domainCache = domainCache;
    }

    @Override
    @CachePut(cacheNames = CacheDomains.ORDEN_COMPRA, key = "#result.id")
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
            // Una sola consulta SQL con WHERE id IN (...) para todos los productos de la orden,
            // evitando N consultas individuales dentro del bucle.
            Set<Long> idsProducto = dto.getDetalles().stream()
                    .map(OrdenCompraDetalleDTO::getIdProducto)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<Long, Producto> productosPorId = productoRepository.findAllById(idsProducto).stream()
                    .collect(Collectors.toMap(Producto::getId, Function.identity()));

            for (OrdenCompraDetalleDTO dDto : dto.getDetalles()) {
                Producto producto = productosPorId.get(dDto.getIdProducto());
                if (producto == null) {
                    throw new ResourceNotFoundException("Producto", "id", dDto.getIdProducto());
                }

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
    @CachePut(cacheNames = CacheDomains.ORDEN_COMPRA, key = "#id")
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
        List<OrdenCompraDTO> ordenes = ordenCompraRepository.findByProveedorId(proveedorId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        domainCache.putAll(CacheDomains.ORDEN_COMPRA, ordenes, OrdenCompraDTO::getId);
        return ordenes;
    }

    @Override
    @Cacheable(cacheNames = CacheDomains.ORDEN_COMPRA, key = "#id")
    @Transactional(readOnly = true)
    public OrdenCompraDTO obtenerPorId(Long id) {
        return super.obtenerPorId(id);
    }

    @Override
    @CachePut(cacheNames = CacheDomains.ORDEN_COMPRA, key = "#id")
    public OrdenCompraDTO actualizar(Long id, OrdenCompraDTO dto) {
        return super.actualizar(id, dto);
    }

    @Override
    @CacheEvict(cacheNames = CacheDomains.ORDEN_COMPRA, key = "#id")
    public void eliminarLogico(Long id) {
        super.eliminarLogico(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<OrdenCompraDTO> listarPaginado(Pageable pageable) {
        PageResponseDTO<OrdenCompraDTO> pagina = super.listarPaginado(pageable);
        domainCache.putAll(CacheDomains.ORDEN_COMPRA, pagina.getContenido(), OrdenCompraDTO::getId);
        return pagina;
    }
}
