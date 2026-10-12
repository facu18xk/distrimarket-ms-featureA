package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.ProveedorProducto;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorProductoRepository extends BaseRepository<ProveedorProducto, Long> {
    Optional<ProveedorProducto> findByProveedorIdAndProductoId(Long proveedorId, Long productoId);
    List<ProveedorProducto> findByProveedorId(Long proveedorId);
}
