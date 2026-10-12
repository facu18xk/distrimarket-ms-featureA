package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.FacturaCompra;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacturaCompraRepository extends BaseRepository<FacturaCompra, Long> {
    List<FacturaCompra> findByProveedorId(Long proveedorId);
    Optional<FacturaCompra> findByProveedorIdAndNumeroFacturaAndTimbrado(Long proveedorId, String numeroFactura, String timbrado);
}
