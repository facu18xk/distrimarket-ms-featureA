package com.distrimarket.compras.controller;

import com.distrimarket.compras.exception.GlobalExceptionHandler;
import com.distrimarket.compras.repository.FacturaCompraRepository;
import com.distrimarket.compras.service.ReporteComprasService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReporteComprasControllerTest {

    @Test
    void rechazaFiltrosInvalidosAntesDeConsultarFacturas() throws Exception {
        var repository = mock(FacturaCompraRepository.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new ReporteComprasController(new ReporteComprasService(repository)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String ruta = "/compras/facturas/reporte";
        mvc.perform(get(ruta)).andExpect(status().isBadRequest());
        mvc.perform(get(ruta).param("desde", "ayer").param("hasta", "2090-01-31"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(ruta).param("desde", "2090-02-01").param("hasta", "2090-01-31"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(ruta).param("desde", "2090-01-01").param("hasta", "2090-01-31")
                .param("estado", "PENDIENTE")).andExpect(status().isBadRequest());
        mvc.perform(get(ruta).param("desde", "2090-01-01").param("hasta", "2090-01-31")
                .param("proveedorId", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get(ruta).param("desde", "2090-01-01").param("hasta", "2090-01-31")
                .param("depositoId", "0")).andExpect(status().isBadRequest());

        verifyNoInteractions(repository);
    }
}
