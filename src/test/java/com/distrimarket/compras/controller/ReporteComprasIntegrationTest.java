package com.distrimarket.compras.controller;

import com.distrimarket.commons.security.JwtClaims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=${REPORTE_TEST_DB_URL}",
        "distrimarket.cache.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "REPORTE_TEST_DB_URL", matches = ".+")
class ReporteComprasIntegrationTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;

    @Test
    @Transactional
    void agregaTodoElPeriodoYAplicaElMismoDepositoATodasLasSecciones() throws Exception {
        String etiqueta = "Reporte " + UUID.randomUUID();
        long persona = insertar("""
                insert into personas (fecha_creacion, tipo_persona, nombre_completo)
                values (now(), 'JURIDICA', ?) returning id
                """, etiqueta);
        long proveedor = insertar("""
                insert into proveedores (fecha_creacion, id_persona, estado)
                values (now(), ?, false) returning id
                """, persona);
        long central = catalogo("depositos", etiqueta);
        long otro = catalogo("depositos", etiqueta + " otro");
        long medioPago = catalogo("medios_pago", etiqueta);
        long producto = insertar("""
                insert into productos (fecha_creacion, nombre, id_categoria, id_marca, activo)
                values (now(), ?, ?, ?, false) returning id
                """, etiqueta, catalogo("categorias", etiqueta), catalogo("marcas", etiqueta));

        jdbc.update("""
                insert into facturas_compras (fecha_creacion, id_proveedor, id_deposito, id_medio_pago,
                       numero_factura, timbrado, fecha_emision, estado, activo, total_general)
                select now(), ?, ?, ?, 'lote-' || n, '1', date '2090-01-01', 'ACTIVA', true, 1
                from generate_series(1, 501) n
                """, proveedor, central, medioPago);
        long ultima = factura(proveedor, central, medioPago, "ultima", "2090-01-31", "ACTIVA", true, 50);
        long anulada = factura(proveedor, central, medioPago, "anulada", "2090-01-31", "ANULADA", true, 70);
        long fuera = factura(proveedor, central, medioPago, "fuera", "2090-02-01", "ACTIVA", true, 100);
        long borrada = factura(proveedor, central, medioPago, "borrada", "2090-01-31", "ACTIVA", false, 100);
        long ajena = factura(proveedor, otro, medioPago, "ajena", "2090-01-31", "ACTIVA", true, 100);
        detalle(ultima, producto, 2, 20);
        detalle(ultima, producto, 3, 30);
        detalle(anulada, producto, 7, 70);
        detalle(borrada, producto, 10, 100);
        detalle(fuera, producto, 10, 100);
        detalle(ajena, producto, 10, 100);

        mvc.perform(reporte(proveedor).param("depositoId", Long.toString(otro))
                        .with(jwt().jwt(token -> token.claim(JwtClaims.DEPOSITO_ID, central))
                                .authorities(new SimpleGrantedAuthority("ROLE_COMPRADOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depositoId").value(central))
                .andExpect(jsonPath("$.resumen.activas").value(502))
                .andExpect(jsonPath("$.resumen.totalComprado").value(551))
                .andExpect(jsonPath("$.resumen.anuladas").value(1))
                .andExpect(jsonPath("$.resumen.totalAnulado").value(70))
                .andExpect(jsonPath("$.porDia[0].fecha").value("2090-01-01"))
                .andExpect(jsonPath("$.porDia[1].fecha").value("2090-01-31"))
                .andExpect(jsonPath("$.porProveedor[0].nombre").value(etiqueta))
                .andExpect(jsonPath("$.porMedioPago[0].total").value(551))
                .andExpect(jsonPath("$.productos[0].nombre").value("Producto #" + producto))
                .andExpect(jsonPath("$.productos[0].unidades").value(5))
                .andExpect(jsonPath("$.productos[0].facturas").value(1))
                .andExpect(jsonPath("$.productos[0].total").value(50));

        var admin = jwt().jwt(token -> token.claim(JwtClaims.DEPOSITO_ID, central))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mvc.perform(reporte(proveedor).param("depositoId", Long.toString(otro)).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.totalComprado").value(100));
        mvc.perform(reporte(proveedor).param("estado", "ANULADA").with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.activas").value(0))
                .andExpect(jsonPath("$.resumen.anuladas").value(1))
                .andExpect(jsonPath("$.productos").isEmpty());
        mvc.perform(reporte(Long.MAX_VALUE).with(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.totalComprado").value(0))
                .andExpect(jsonPath("$.porProveedor").isEmpty());
    }

    @Test
    void rechazaUsuariosSinPermisoDeCompras() throws Exception {
        mvc.perform(reporte(1)).andExpect(status().isUnauthorized());
        mvc.perform(reporte(1).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VENDEDOR"))))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder reporte(long proveedor) {
        return get("/compras/facturas/reporte").param("desde", "2090-01-01").param("hasta", "2090-01-31")
                .param("proveedorId", Long.toString(proveedor));
    }

    private long catalogo(String tabla, String nombre) {
        return insertar("insert into " + tabla + " (fecha_creacion, nombre) values (now(), ?) returning id", nombre);
    }

    private long factura(long proveedor, long deposito, long medioPago, String numero, String fecha,
                         String estado, boolean activo, int monto) {
        return insertar("""
                insert into facturas_compras (fecha_creacion, id_proveedor, id_deposito, id_medio_pago,
                       numero_factura, timbrado, fecha_emision, estado, activo, total_general)
                values (now(), ?, ?, ?, ?, '1', cast(? as date), ?, ?, ?) returning id
                """, proveedor, deposito, medioPago, numero, fecha, estado, activo, monto);
    }

    private void detalle(long factura, long producto, int cantidad, int total) {
        jdbc.update("""
                insert into facturas_compras_detalle (fecha_creacion, id_factura_compra, id_producto,
                       cantidad, precio_unitario, subtotal) values (now(), ?, ?, ?, 10, ?)
                """, factura, producto, cantidad, total);
    }

    private long insertar(String sql, Object... parametros) {
        return jdbc.queryForObject(sql, Long.class, parametros);
    }
}
