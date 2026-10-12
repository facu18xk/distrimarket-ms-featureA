package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.Persona;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonaRepository extends BaseRepository<Persona, Long> {
    Optional<Persona> findByCi(String ci);
    Optional<Persona> findByRuc(String ruc);
}
