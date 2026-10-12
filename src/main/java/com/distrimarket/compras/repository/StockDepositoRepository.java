package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.StockDeposito;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockDepositoRepository extends BaseRepository<StockDeposito, Long> {
    Optional<StockDeposito> findByDepositoIdAndProductoId(Long depositoId, Long productoId);
}
