package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProveedorRepository extends BaseRepository<Proveedor, Long> {

    /**
     * @EntityGraph precarga la relación @OneToOne LAZY "persona" mediante LEFT JOIN
     * en el listado paginado, evitando N consultas adicionales a la tabla personas.
     */
    @Override
    @EntityGraph(attributePaths = {"persona"})
    Page<Proveedor> findAll(Pageable pageable);

    /**
     * @EntityGraph precarga "persona" cuando se filtra con Specification paginado,
     * evitando el problema N+1 al mapear Proveedor -> ProveedorDTO.
     */
    @Override
    @EntityGraph(attributePaths = {"persona"})
    Page<Proveedor> findAll(Specification<Proveedor> spec, Pageable pageable);

    /**
     * @EntityGraph precarga "persona" al buscar un proveedor por su ID en una sola consulta SQL.
     */
    @Override
    @EntityGraph(attributePaths = {"persona"})
    Optional<Proveedor> findById(Long id);

    /**
     * @EntityGraph precarga "persona" al buscar proveedor por ID de persona.
     */
    @EntityGraph(attributePaths = {"persona"})
    Optional<Proveedor> findByPersonaId(Long personaId);

    /**
     * @EntityGraph precarga "persona" al buscar proveedor por RUC, evitando una segunda consulta.
     */
    @EntityGraph(attributePaths = {"persona"})
    Optional<Proveedor> findByPersonaRuc(String ruc);
}
