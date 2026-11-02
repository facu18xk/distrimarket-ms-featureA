package com.distrimarket.compras.controller;

import com.distrimarket.compras.service.OrdenCompraService;
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
class OrdenCompraControllerTest {

    @Mock
    private OrdenCompraService ordenCompraService;

    @InjectMocks
    private OrdenCompraController controller;

    @Test
    void listadoRaizAplicaFiltrosYOrdenaConDesempatePorId() {
        Pageable pedido = PageRequest.of(1, 25, Sort.by(Sort.Direction.DESC, "total"));

        controller.listar("oc", 3L, "pendiente", pedido);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ordenCompraService).buscarPaginado(eq("oc"), eq(3L), eq("pendiente"),
                captor.capture());

        Pageable paginacion = captor.getValue();
        assertThat(paginacion.getPageNumber()).isEqualTo(1);
        assertThat(paginacion.getPageSize()).isEqualTo(25);
        assertThat(paginacion.getSort().getOrderFor("total").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(paginacion.getSort().getOrderFor("id").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void sinOrdenUsaIdAscendente() {
        controller.listar(null, null, null, PageRequest.of(0, 10));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ordenCompraService).buscarPaginado(nullable(String.class), isNull(), isNull(),
                captor.capture());

        assertThat(captor.getValue().getSort().toList()).containsExactly(Sort.Order.asc("id"));
    }

    @Test
    void rechazaOrdenNoPermitido() {
        Pageable pedido = PageRequest.of(0, 10, Sort.by("observacion"));

        assertThatThrownBy(() -> controller.listar(null, null, null, pedido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("observacion");

        verifyNoInteractions(ordenCompraService);
    }
}
