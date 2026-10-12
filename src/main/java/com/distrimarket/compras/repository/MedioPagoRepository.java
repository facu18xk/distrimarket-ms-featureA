package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.MedioPago;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedioPagoRepository extends BaseRepository<MedioPago, Long> {
    Optional<MedioPago> findByNombre(String nombre);
}
