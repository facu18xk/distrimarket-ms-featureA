package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.Producto;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductoRepository extends BaseRepository<Producto, Long> {
    Optional<Producto> findByCodigoBarra(String codigoBarra);
}
