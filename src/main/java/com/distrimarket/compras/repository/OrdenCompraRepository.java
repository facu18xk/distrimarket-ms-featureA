package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.OrdenCompra;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenCompraRepository extends BaseRepository<OrdenCompra, Long> {

    /**
     * @EntityGraph precarga la colección LAZY "detalles" en un único SELECT con LEFT JOIN
     * al consultar una orden de compra por ID, evitando una consulta adicional en el mapper.
     */
    @Override
    @EntityGraph(attributePaths = {"detalles"})
    Optional<OrdenCompra> findById(Long id);

    /**
     * @EntityGraph precarga los detalles de todas las órdenes del proveedor en una sola consulta,
     * evitando N consultas adicionales al convertir a DTO.
     */
    @EntityGraph(attributePaths = {"detalles"})
    List<OrdenCompra> findByProveedorId(Long proveedorId);

    /**
     * @EntityGraph precarga los detalles al filtrar órdenes por estado.
     */
    @EntityGraph(attributePaths = {"detalles"})
    List<OrdenCompra> findByEstado(String estado);
}
