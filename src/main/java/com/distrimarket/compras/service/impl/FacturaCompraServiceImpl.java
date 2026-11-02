package com.distrimarket.compras.service.impl;

import com.distrimarket.commons.security.UsuarioActual;
import com.distrimarket.compras.security.DepositoVisible;
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
import com.distrimarket.commons.service.BusquedaTexto;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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
        // Quien no es ADMIN compra siempre para el depósito asignado en su token.
        dto.setIdDeposito(UsuarioActual.depositoParaOperar(dto.getIdDeposito()));
        log.info("Registrando factura de compra: Nro {}, Timbrado {}, Proveedor ID {}",
                dto.getNumeroFactura(), dto.getTimbrado(), dto.getIdProveedor());

        // El timbrado del proveedor es opcional (el formulario ya no lo pide).
        if (dto.getTimbrado() != null && dto.getTimbrado().isBlank()) {
            dto.setTimbrado(null);
        }
        if (dto.getTimbrado() == null) {
            if (facturaCompraRepository.existsByProveedorIdAndNumeroFactura(dto.getIdProveedor(), dto.getNumeroFactura())) {
                throw new ConflictException("Ya existe una factura registrada con ese número para el proveedor indicado");
            }
        } else {
            facturaCompraRepository.findByProveedorIdAndNumeroFacturaAndTimbrado(
                    dto.getIdProveedor(), dto.getNumeroFactura(), dto.getTimbrado()
            ).ifPresent(f -> {
                throw new ConflictException("Ya existe una factura registrada con ese número y timbrado para el proveedor indicado");
            });
        }

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
            // Carga en lote (1 sola consulta SQL con WHERE id IN (...)) de todos los productos
            // y del stock actual del depósito, evitando 2*N SELECT y N SAVE dentro del bucle.
            Set<Long> idsProducto = dto.getDetalles().stream()
                    .map(FacturaCompraDetalleRequestDTO::getIdProducto)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<Long, Producto> productosPorId = productoRepository.findAllById(idsProducto).stream()
                    .collect(Collectors.toMap(Producto::getId, Function.identity()));

            Map<Long, StockDeposito> stockPorProductoId = stockDepositoRepository
                    .findByDepositoIdAndProductoIdIn(deposito.getId(), idsProducto).stream()
                    .collect(Collectors.toMap(sd -> sd.getProducto().getId(), Function.identity()));

            for (FacturaCompraDetalleRequestDTO dDto : dto.getDetalles()) {
                Producto producto = productosPorId.get(dDto.getIdProducto());
                if (producto == null) {
                    throw new ResourceNotFoundException("Producto", "id", dDto.getIdProducto());
                }

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

                // Actualización acumulada de existencias en depósito en memoria
                StockDeposito stock = stockPorProductoId.computeIfAbsent(producto.getId(), idProd ->
                        StockDeposito.builder()
                                .deposito(deposito)
                                .producto(producto)
                                .cantidad(0)
                                .build());

                stock.setCantidad(stock.getCantidad() + dDto.getCantidad());
                log.info("Inventario incrementado para producto ID: {} en depósito ID: {}. Nueva cantidad: {}",
                        producto.getId(), deposito.getId(), stock.getCantidad());
            }

            stockDepositoRepository.saveAll(stockPorProductoId.values());
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
                .filter(f -> DepositoVisible.puedeVer(f.getDeposito() == null ? null : f.getDeposito().getId()))
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
        exigirVisible(factura);
        factura.setEstado("ANULADA");
        FacturaCompra actualizada = facturaCompraRepository.save(factura);
        return mapper.toDto(actualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<FacturaCompraResponseDTO> buscarPaginado(
            Long proveedorId, String estado, LocalDate desde, LocalDate hasta,
            String numeroFactura, Pageable pageable) {
        return buscarPaginado(null, proveedorId, estado, desde, hasta, numeroFactura, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<FacturaCompraResponseDTO> buscarPaginado(
            String q, Long proveedorId, String estado, LocalDate desde, LocalDate hasta,
            String numeroFactura, Pageable pageable) {

        log.debug("Buscando facturas de compra con filtros: q={}, proveedorId={}, estado={}, desde={}, hasta={}, numeroFactura={}",
                q, proveedorId, estado, desde, hasta, numeroFactura);
        validarRangoFechas(desde, hasta);

        Long depositoVisible = DepositoVisible.filtro(null);
        boolean sinFiltros = depositoVisible == null
                && proveedorId == null
                && (estado == null || estado.isBlank())
                && desde == null
                && hasta == null
                && (numeroFactura == null || numeroFactura.isBlank());
        if (sinFiltros && (q == null || q.isBlank())) {
            // El listado raíz sin filtros ni texto conserva el comportamiento de siempre.
            return listarPaginado(pageable);
        }

        Specification<FacturaCompra> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Quien tiene depósito asignado (y no es ADMIN) solo ve las facturas de su depósito.
            if (depositoVisible != null) {
                predicates.add(cb.equal(root.get("deposito").get("id"), depositoVisible));
            }
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

        if (q != null && !q.isBlank()) {
            spec = spec.and(BusquedaTexto.<FacturaCompra>en(q, camposDeBusqueda()));
        }

        return buscarPaginado(spec, pageable);
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }
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
        facturaCompraRepository.findById(id).ifPresent(this::exigirVisible);
        dto.setIdDeposito(UsuarioActual.depositoParaOperar(dto.getIdDeposito()));
        return super.actualizar(id, dto);
    }

    @Override
    @CacheEvict(cacheNames = CacheDomains.FACTURA_COMPRA, key = "#id")
    public void eliminarLogico(Long id) {
        facturaCompraRepository.findById(id).ifPresent(this::exigirVisible);
        super.eliminarLogico(id);
    }

    /** Los listados se arman desde la caché de este dominio, objeto por objeto. */
    @Override
    protected String dominioCache() {
        return CacheDomains.FACTURA_COMPRA;
    }

    /** Campos donde busca el parámetro q del listado. */
    @Override
    protected String[] camposDeBusqueda() {
        return new String[]{"numeroFactura", "timbrado", "proveedor.nombreFantasia", "proveedor.persona.nombreCompleto"};
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<FacturaCompraResponseDTO> listarPaginado(Pageable pageable) {
        // Limitado a su depósito: el listado sin filtros también se acota.
        Long depositoVisible = DepositoVisible.filtro(null);
        if (depositoVisible != null) {
            return buscarPaginado((root, query, cb) -> cb.equal(root.get("deposito").get("id"), depositoVisible), pageable);
        }
        PageResponseDTO<FacturaCompraResponseDTO> pagina = super.listarPaginado(pageable);
        // La página ya se armó desde la caché, objeto por objeto (ver dominioCache): no se vuelve a guardar.
        return pagina;
    }

    /** Una factura de otro depósito "no existe" para quien está limitado a su depósito. */
    private void exigirVisible(FacturaCompra factura) {
        if (!DepositoVisible.puedeVer(factura.getDeposito() == null ? null : factura.getDeposito().getId())) {
            throw new ResourceNotFoundException("FacturaCompra", "id", factura.getId());
        }
    }
}
