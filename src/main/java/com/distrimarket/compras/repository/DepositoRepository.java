package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.Deposito;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepositoRepository extends BaseRepository<Deposito, Long> {
    Optional<Deposito> findByNombre(String nombre);
}
