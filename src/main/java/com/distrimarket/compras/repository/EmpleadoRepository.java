package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.Empleado;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends BaseRepository<Empleado, Long> {
    Optional<Empleado> findByPersonaId(Long personaId);
}
