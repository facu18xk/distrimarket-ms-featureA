package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProveedorRepository extends BaseRepository<Proveedor, Long> {
    Optional<Proveedor> findByPersonaId(Long personaId);
    Optional<Proveedor> findByPersonaRuc(String ruc);
}
