package com.distrimarket.compras.repository;

import com.distrimarket.commons.entity.OrdenCompra;
import com.distrimarket.commons.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenCompraRepository extends BaseRepository<OrdenCompra, Long> {
    List<OrdenCompra> findByProveedorId(Long proveedorId);
    List<OrdenCompra> findByEstado(String estado);
}
