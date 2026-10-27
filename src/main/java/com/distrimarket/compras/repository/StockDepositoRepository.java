package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.StockDeposito;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockDepositoRepository extends BaseRepository<StockDeposito, Long> {
    Optional<StockDeposito> findByDepositoIdAndProductoId(Long depositoId, Long productoId);

    /**
     * Recupera en una sola consulta SQL (WHERE id_deposito = ? AND id_producto IN (...))
     * el stock de todos los productos de una factura, evitando N consultas dentro del bucle.
     */
    List<StockDeposito> findByDepositoIdAndProductoIdIn(Long depositoId, Collection<Long> productoIds);
}
