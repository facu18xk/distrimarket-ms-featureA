package com.distrimarket.compras.service.impl;

import com.distrimarket.commons.dto.FacturaCompraDetalleRequestDTO;
import com.distrimarket.commons.dto.FacturaCompraRequestDTO;
import com.distrimarket.commons.dto.FacturaCompraResponseDTO;
import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.entity.Deposito;
import com.distrimarket.commons.entity.FacturaCompra;
import com.distrimarket.commons.entity.FacturaCompraDetalle;
import com.distrimarket.commons.entity.MedioPago;
import com.distrimarket.commons.entity.OrdenCompra;
import com.distrimarket.commons.entity.Producto;
import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.commons.entity.StockDeposito;
import com.distrimarket.commons.exception.ResourceNotFoundException;
import com.distrimarket.commons.service.impl.BaseServiceImpl;
import com.distrimarket.compras.config.cache.CacheDomains;
import com.distrimarket.compras.config.cache.DomainCache;
import com.distrimarket.compras.exception.ConflictException;
import com.distrimarket.compras.mapper.FacturaCompraMapper;
import com.distrimarket.compras.repository.DepositoRepository;
import com.distrimarket.compras.repository.FacturaCompraRepository;
import com.distrimarket.compras.repository.MedioPagoRepository;
import com.distrimarket.compras.repository.OrdenCompraRepository;
import com.distrimarket.compras.repository.ProductoRepository;
import com.distrimarket.compras.repository.ProveedorRepository;
import com.distrimarket.compras.repository.StockDepositoRepository;
import com.distrimarket.compras.service.FacturaCompraService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class FacturaCompraServiceImpl extends BaseServiceImpl<FacturaCompra, Long, FacturaCompraRequestDTO, FacturaCompraResponseDTO> implements FacturaCompraService {

    private final FacturaCompraRepository facturaCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final DepositoRepository depositoRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final ProductoRepository productoRepository;
    private final StockDepositoRepository stockDepositoRepository;
    private final DomainCache domainCache;

    public FacturaCompraServiceImpl(
            FacturaCompraRepository facturaCompraRepository,
            FacturaCompraMapper facturaCompraMapper,
            ProveedorRepository proveedorRepository,
            DepositoRepository depositoRepository,
            MedioPagoRepository medioPagoRepository,
            OrdenCompraRepository ordenCompraRepository,
            ProductoRepository productoRepository,
            StockDepositoRepository stockDepositoRepository,
            DomainCache domainCache) {
        super(facturaCompraRepository, facturaCompraMapper);
        this.facturaCompraRepository = facturaCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.depositoRepository = depositoRepository;
        this.medioPagoRepository = medioPagoRepository;
        this.ordenCompraRepository = ordenCompraRepository;
        this.productoRepository = productoRepository;
        this.stockDepositoRepository = stockDepositoRepository;
        this.domainCache = domainCache;
    }

    @Override
    @CachePut(cacheNames = CacheDomains.FACTURA_COMPRA, key = "#result.idFacturaCompra")
    public FacturaCompraResponseDTO crear(FacturaCompraRequestDTO dto) {
        log.info("Registrando factura de compra: Nro {}, Timbrado {}, Proveedor ID {}",
                dto.getNumeroFactura(), dto.getTimbrado(), dto.getIdProveedor());

        facturaCompraRepository.findByProveedorIdAndNumeroFacturaAndTimbrado(
                dto.getIdProveedor(), dto.getNumeroFactura(), dto.getTimbrado()
        ).ifPresent(f -> {
            throw new ConflictException("Ya existe una factura registrada con ese número y timbrado para el proveedor indicado");
        });

        Proveedor proveedor = proveedorRepository.findById(dto.getIdProveedor())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", "id", dto.getIdProveedor()));

        Deposito deposito = depositoRepository.findById(dto.getIdDeposito())
                .orElseThrow(() -> new ResourceNotFoundException("Deposito", "id", dto.getIdDeposito()));

        MedioPago medioPago = medioPagoRepository.findById(dto.getIdMedioPago())
                .orElseThrow(() -> new ResourceNotFoundException("MedioPago", "id", dto.getIdMedioPago()));

        OrdenCompra ordenCompra = null;
        if (dto.getIdOrdenCompra() != null) {
            ordenCompra = ordenCompraRepository.findById(dto.getIdOrdenCompra())
                    .orElseThrow(() -> new ResourceNotFoundException("OrdenCompra", "id", dto.getIdOrdenCompra()));
        }

        String condicion = dto.getCondicionCompra() != null ? dto.getCondicionCompra().getValue() : "CONTADO";

        FacturaCompra factura = FacturaCompra.builder()
                .proveedor(proveedor)
                .deposito(deposito)
                .medioPago(medioPago)
                .ordenCompra(ordenCompra)
                .numeroFactura(dto.getNumeroFactura())
                .fechaEmision(dto.getFechaEmision())
                .timbrado(dto.getTimbrado())
                .condicionCompra(condicion)
                .estado("ACTIVA")
                .totalGeneral(BigDecimal.ZERO)
                .totalIva(BigDecimal.ZERO)
                .detalles(new ArrayList<>())
                .build();

        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
            for (FacturaCompraDetalleRequestDTO dDto : dto.getDetalles()) {
                Producto producto = productoRepository.findById(dDto.getIdProducto())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", dDto.getIdProducto()));

                BigDecimal iva = (dDto.getPorcentajeIva() != null && dDto.getPorcentajeIva().getValue() != null)
                        ? dDto.getPorcentajeIva().getValue()
                        : new BigDecimal("10.00");

                FacturaCompraDetalle detalle = FacturaCompraDetalle.builder()
                        .producto(producto)
                        .cantidad(dDto.getCantidad())
                        .precioUnitario(dDto.getPrecioUnitario() != null ? dDto.getPrecioUnitario() : BigDecimal.ZERO)
                        .porcentajeIva(iva)
                        .build();

                detalle.calcularSubtotal();
                factura.agregarDetalle(detalle);

                // Actualización automática de existencias en depósito
                StockDeposito stock = stockDepositoRepository
                        .findByDepositoIdAndProductoId(deposito.getId(), producto.getId())
                        .orElseGet(() -> StockDeposito.builder()
                                .deposito(deposito)
                                .producto(producto)
                                .cantidad(0)
                                .build());

                stock.setCantidad(stock.getCantidad() + dDto.getCantidad());
                stockDepositoRepository.save(stock);
                log.info("Inventario incrementado para producto ID: {} en depósito ID: {}. Nueva cantidad: {}",
                        producto.getId(), deposito.getId(), stock.getCantidad());
            }
        }

        factura.recalcularTotales();
        FacturaCompra guardada = facturaCompraRepository.save(factura);
        log.info("Factura de compra registrada con éxito. ID: {}, Total: {}", guardada.getId(), guardada.getTotalGeneral());

        return mapper.toDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaCompraResponseDTO> listarPorProveedor(Long proveedorId) {
        List<FacturaCompraResponseDTO> facturas = facturaCompraRepository.findByProveedorId(proveedorId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        domainCache.putAll(CacheDomains.FACTURA_COMPRA, facturas,
                FacturaCompraResponseDTO::getIdFacturaCompra);
        return facturas;
    }

    @Override
    @CachePut(cacheNames = CacheDomains.FACTURA_COMPRA, key = "#id")
    public FacturaCompraResponseDTO anularFactura(Long id) {
        log.info("Anulando factura de compra ID: {}", id);
        FacturaCompra factura = facturaCompraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FacturaCompra", "id", id));
        factura.setEstado("ANULADA");
        FacturaCompra actualizada = facturaCompraRepository.save(factura);
        return mapper.toDto(actualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<FacturaCompraResponseDTO> buscarPaginado(
            Long proveedorId, String estado, LocalDate desde, LocalDate hasta,
            String numeroFactura, Pageable pageable) {

        log.debug("Buscando facturas de compra con filtros: proveedorId={}, estado={}, desde={}, hasta={}, numeroFactura={}",
                proveedorId, estado, desde, hasta, numeroFactura);

        Specification<FacturaCompra> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (proveedorId != null) {
                predicates.add(cb.equal(root.get("proveedor").get("id"), proveedorId));
            }
            if (estado != null && !estado.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("estado")), estado.trim().toUpperCase()));
            }
            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), desde));
            }
            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaEmision"), hasta));
            }
            if (numeroFactura != null && !numeroFactura.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("numeroFactura")),
                        "%" + numeroFactura.trim().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageResponseDTO<FacturaCompraResponseDTO> pagina = buscarPaginado(spec, pageable);
        domainCache.putAll(CacheDomains.FACTURA_COMPRA, pagina.getContenido(),
                FacturaCompraResponseDTO::getIdFacturaCompra);
        return pagina;
    }

    @Override
    @Cacheable(cacheNames = CacheDomains.FACTURA_COMPRA, key = "#id")
    @Transactional(readOnly = true)
    public FacturaCompraResponseDTO obtenerPorId(Long id) {
        return super.obtenerPorId(id);
    }

    @Override
    @CachePut(cacheNames = CacheDomains.FACTURA_COMPRA, key = "#id")
    public FacturaCompraResponseDTO actualizar(Long id, FacturaCompraRequestDTO dto) {
        return super.actualizar(id, dto);
    }

    @Override
    @CacheEvict(cacheNames = CacheDomains.FACTURA_COMPRA, key = "#id")
    public void eliminarLogico(Long id) {
        super.eliminarLogico(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<FacturaCompraResponseDTO> listarPaginado(Pageable pageable) {
        PageResponseDTO<FacturaCompraResponseDTO> pagina = super.listarPaginado(pageable);
        domainCache.putAll(CacheDomains.FACTURA_COMPRA, pagina.getContenido(),
                FacturaCompraResponseDTO::getIdFacturaCompra);
        return pagina;
    }
}
