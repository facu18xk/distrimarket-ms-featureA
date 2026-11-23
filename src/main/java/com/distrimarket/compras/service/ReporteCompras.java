package com.distrimarket.compras.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteCompras(
        Long depositoId, Resumen resumen, List<Dia> porDia,
        List<Grupo> porProveedor, List<Grupo> porMedioPago, List<Producto> productos) {

    public record Resumen(long activas, BigDecimal totalComprado, long anuladas, BigDecimal totalAnulado) {}

    public record Dia(LocalDate fecha, long facturas, BigDecimal total) {}

    public record Grupo(Long id, String nombre, long facturas, BigDecimal total) {}

    public record Producto(Long id, String nombre, long facturas, long unidades, BigDecimal total) {}
}
