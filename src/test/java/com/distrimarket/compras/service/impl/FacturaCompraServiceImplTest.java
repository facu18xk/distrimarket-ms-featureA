package com.distrimarket.compras.service.impl;

import com.distrimarket.compras.config.cache.DomainCache;
import com.distrimarket.compras.mapper.FacturaCompraMapper;
import com.distrimarket.compras.repository.DepositoRepository;
import com.distrimarket.compras.repository.FacturaCompraRepository;
import com.distrimarket.compras.repository.MedioPagoRepository;
import com.distrimarket.compras.repository.OrdenCompraRepository;
import com.distrimarket.compras.repository.ProductoRepository;
import com.distrimarket.compras.repository.ProveedorRepository;
import com.distrimarket.compras.repository.StockDepositoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FacturaCompraServiceImplTest {

    private FacturaCompraServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FacturaCompraServiceImpl(
                mock(FacturaCompraRepository.class),
                mock(FacturaCompraMapper.class),
                mock(ProveedorRepository.class),
                mock(DepositoRepository.class),
                mock(MedioPagoRepository.class),
                mock(OrdenCompraRepository.class),
                mock(ProductoRepository.class),
                mock(StockDepositoRepository.class),
                mock(DomainCache.class));
    }

    @Test
    void rechazaRangoDeFechasInvertido() {
        LocalDate desde = LocalDate.of(2026, 6, 30);
        LocalDate hasta = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> service.buscarPaginado(
                null, null, null, desde, hasta, null, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("desde")
                .hasMessageContaining("hasta");
    }

    @Test
    void rechazaRangoInvertidoTambienEnListadoConBusqueda() {
        LocalDate desde = LocalDate.of(2026, 6, 30);
        LocalDate hasta = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> service.buscarPaginado(
                "acme", 1L, "ACTIVA", desde, hasta, null, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("desde")
                .hasMessageContaining("hasta");
    }
}
