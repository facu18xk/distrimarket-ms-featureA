package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.FacturaCompra;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

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
}
