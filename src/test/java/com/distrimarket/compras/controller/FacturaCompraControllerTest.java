package com.distrimarket.compras.controller;

import com.distrimarket.compras.service.FacturaCompraService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FacturaCompraControllerTest {

    @Mock
    private FacturaCompraService facturaCompraService;

    @InjectMocks
    private FacturaCompraController controller;

    @Test
    void listadoRaizAplicaFiltrosYOrdenaConDesempatePorId() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        Pageable pedido = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fechaEmision"));

        controller.listar("acme", 7L, "activa", desde, hasta, "001", pedido);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(facturaCompraService).buscarPaginado(nullable(String.class), eq(7L), eq("activa"),
                eq(desde), eq(hasta), eq("001"), captor.capture());

        Pageable paginacion = captor.getValue();
        assertThat(paginacion.getSort().getOrderFor("fechaEmision").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(paginacion.getSort().getOrderFor("id").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void buscarUsaLaMismaAllowlistDeOrden() {
        Pageable pedido = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "totalGeneral"));

        controller.buscar(null, null, null, null, null, pedido);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(facturaCompraService).buscarPaginado(isNull(), isNull(), isNull(), isNull(),
                isNull(), captor.capture());

        assertThat(captor.getValue().getSort().getOrderFor("totalGeneral").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(captor.getValue().getSort().getOrderFor("id").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void sinOrdenUsaIdAscendente() {
        controller.listar(null, null, null, null, null, null, PageRequest.of(0, 10));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(facturaCompraService).buscarPaginado(nullable(String.class), isNull(), isNull(),
                isNull(), isNull(), isNull(), captor.capture());

        assertThat(captor.getValue().getSort().toList()).containsExactly(Sort.Order.asc("id"));
    }

    @Test
    void rechazaOrdenNoPermitido() {
        Pageable pedido = PageRequest.of(0, 10, Sort.by("password"));

        assertThatThrownBy(() -> controller.listar(null, null, null, null, null, null, pedido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");

        verifyNoInteractions(facturaCompraService);
    }
}
