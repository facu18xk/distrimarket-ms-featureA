package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.FacturaCompra;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaCompraRepository extends BaseRepository<FacturaCompra, Long> {

    /**
     * @EntityGraph fuerza un LEFT JOIN FETCH en una sola consulta SQL de las relaciones
     * de cabecera (proveedor, persona del proveedor, depósito y medio de pago).
     * No incluye la colección "detalles" en consultas paginadas para evitar que Hibernate
     * pagine en memoria (HHH90003004); los detalles y sus productos se cargan en lote
     * mediante hibernate.default_batch_fetch_size.
     */
    @Override
    @EntityGraph(attributePaths = {"proveedor", "proveedor.persona", "deposito", "medioPago"})
    Page<FacturaCompra> findAll(Pageable pageable);

    /**
     * @EntityGraph carga en la misma consulta paginada con filtros (Specification) el proveedor,
     * su persona, el depósito y el medio de pago, evitando 4*N consultas adicionales por página.
     */
    @Override
    @EntityGraph(attributePaths = {"proveedor", "proveedor.persona", "deposito", "medioPago"})
    Page<FacturaCompra> findAll(Specification<FacturaCompra> spec, Pageable pageable);

    /**
     * @EntityGraph en consulta puntual por ID: trae cabecera, líneas de detalle y el producto
     * de cada línea en un único SELECT con LEFT JOIN, evitando 1 + 5 + M consultas.
     */
    @Override
    @EntityGraph(attributePaths = {"proveedor", "proveedor.persona", "deposito", "medioPago", "detalles", "detalles.producto"})
    Optional<FacturaCompra> findById(Long id);

    /**
     * @EntityGraph precarga todas las asociaciones necesarias para el mapper al listar por proveedor,
     * resolviendo cabeceras, detalles y productos en una sola consulta SQL.
     */
    @EntityGraph(attributePaths = {"proveedor", "proveedor.persona", "deposito", "medioPago", "detalles", "detalles.producto"})
    List<FacturaCompra> findByProveedorId(Long proveedorId);

    Optional<FacturaCompra> findByProveedorIdAndNumeroFacturaAndTimbrado(Long proveedorId, String numeroFactura, String timbrado);

    /** Sin timbrado (ya no se pide): el número de factura no se repite para el mismo proveedor. */
    boolean existsByProveedorIdAndNumeroFactura(Long proveedorId, String numeroFactura);

    String FILTROS_REPORTE = """
            where f.activo = true and f.fechaEmision between :desde and :hasta
              and (:proveedorId is null or fk(f.proveedor) = :proveedorId)
              and (:depositoId is null or fk(f.deposito) = :depositoId)
            """;

    interface TotalesPorDia {
        LocalDate getFecha();
        String getEstado();
        long getFacturas();
        BigDecimal getTotal();
    }

    interface GrupoCompras {
        Long getId();
        String getNombre();
        long getFacturas();
        BigDecimal getTotal();
    }

    interface ProductoComprado extends GrupoCompras {
        long getUnidades();
    }

    @Query("""
            select f.fechaEmision as fecha, upper(f.estado) as estado,
                   count(f.id) as facturas, sum(f.totalGeneral) as total
            from FacturaCompra f
            """ + FILTROS_REPORTE + """
              and upper(f.estado) in ('ACTIVA', 'ANULADA')
              and (:estado is null or upper(f.estado) = :estado)
            group by f.fechaEmision, upper(f.estado)
            order by f.fechaEmision
            """)
    List<TotalesPorDia> totalesPorDia(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                                     @Param("proveedorId") Long proveedorId, @Param("depositoId") Long depositoId,
                                     @Param("estado") String estado);

    @Query("""
            select pr.id as id, coalesce(nullif(trim(pr.nombreFantasia), ''), persona.nombreCompleto) as nombre,
                   count(f.id) as facturas, sum(f.totalGeneral) as total
            from FacturaCompra f join Proveedor pr on pr.id = fk(f.proveedor) left join pr.persona persona
            """ + FILTROS_REPORTE + """
              and upper(f.estado) = 'ACTIVA'
            group by pr.id, pr.nombreFantasia, persona.nombreCompleto
            order by sum(f.totalGeneral) desc, pr.id
            """)
    List<GrupoCompras> comprasPorProveedor(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                                           @Param("proveedorId") Long proveedorId,
                                           @Param("depositoId") Long depositoId);

    @Query("""
            select m.id as id, m.nombre as nombre, count(f.id) as facturas, sum(f.totalGeneral) as total
            from FacturaCompra f join f.medioPago m
            """ + FILTROS_REPORTE + """
              and upper(f.estado) = 'ACTIVA'
            group by m.id, m.nombre
            order by sum(f.totalGeneral) desc, m.id
            """)
    List<GrupoCompras> comprasPorMedioPago(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                                           @Param("proveedorId") Long proveedorId,
                                           @Param("depositoId") Long depositoId);

    @Query("""
            select d.producto.id as id, p.nombre as nombre, count(distinct f.id) as facturas,
                   sum(d.cantidad) as unidades, sum(d.subtotal) as total
            from FacturaCompraDetalle d join d.facturaCompra f
            left join Producto p on p.id = fk(d.producto)
            """ + FILTROS_REPORTE + """
              and upper(f.estado) = 'ACTIVA'
            group by d.producto.id, p.nombre
            order by sum(d.cantidad) desc, sum(d.subtotal) desc, d.producto.id
            """)
    List<ProductoComprado> productosComprados(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                                               @Param("proveedorId") Long proveedorId,
                                               @Param("depositoId") Long depositoId);
}
