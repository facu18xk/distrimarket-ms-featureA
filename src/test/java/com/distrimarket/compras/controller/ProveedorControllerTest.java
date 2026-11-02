package com.distrimarket.compras.controller;

import com.distrimarket.compras.service.ProveedorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProveedorControllerTest {

    @Mock
    private ProveedorService proveedorService;

    @InjectMocks
    private ProveedorController controller;

    @Test
    void conservaAliasFechaEmisionComoFechaCreacion() {
        Pageable pedido = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fechaEmision"));

        controller.listarPaginado(null, null, pedido);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(proveedorService).buscarPaginado(nullable(String.class), isNull(), captor.capture());

        Pageable paginacion = captor.getValue();
        assertThat(paginacion.getSort().getOrderFor("fechaEmision")).isNull();
        assertThat(paginacion.getSort().getOrderFor("fechaCreacion").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(paginacion.getSort().getOrderFor("id").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listadoRaizFiltraPorEstadoYConservaBusquedaLibre() {
        Pageable pedido = PageRequest.of(0, 15, Sort.by(Sort.Direction.ASC, "nombreFantasia"));

        controller.listarPaginado("acme", true, pedido);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(proveedorService).buscarPaginado(eq("acme"), eq(true), captor.capture());

        assertThat(captor.getValue().getSort().getOrderFor("nombreFantasia").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        assertThat(captor.getValue().getSort().getOrderFor("id").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void rechazaOrdenNoPermitido() {
        Pageable pedido = PageRequest.of(0, 10, Sort.by("persona.ruc"));

        assertThatThrownBy(() -> controller.listarPaginado(null, null, pedido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("persona.ruc");

        verifyNoInteractions(proveedorService);
    }
}
