package com.distrimarket.compras.service;

import com.distrimarket.compras.exception.BadRequestException;
import com.distrimarket.compras.repository.FacturaCompraRepository;
import com.distrimarket.compras.security.DepositoVisible;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReporteComprasService {

    public enum Estado { ACTIVA, ANULADA }

    private final FacturaCompraRepository repository;

    public ReporteComprasService(FacturaCompraRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReporteCompras generar(LocalDate desde, LocalDate hasta, Long proveedorId, Long depositoId, Estado estado) {
        if (desde.isAfter(hasta)) {
            throw new BadRequestException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
        }
        if ((proveedorId != null && proveedorId <= 0) || (depositoId != null && depositoId <= 0)) {
            throw new BadRequestException("Los identificadores de proveedor y depósito deben ser positivos.");
        }

        Long depositoVisible = DepositoVisible.filtro(depositoId);
        var porDia = new ArrayList<ReporteCompras.Dia>();
        long activas = 0;
        long anuladas = 0;
        BigDecimal totalComprado = BigDecimal.ZERO;
        BigDecimal totalAnulado = BigDecimal.ZERO;

        for (var fila : repository.totalesPorDia(desde, hasta, proveedorId, depositoVisible,
                estado == null ? null : estado.name())) {
            if (Estado.ACTIVA.name().equals(fila.getEstado())) {
                activas += fila.getFacturas();
                totalComprado = totalComprado.add(fila.getTotal());
                porDia.add(new ReporteCompras.Dia(fila.getFecha(), fila.getFacturas(), fila.getTotal()));
            } else {
                anuladas += fila.getFacturas();
                totalAnulado = totalAnulado.add(fila.getTotal());
            }
        }
        var resumen = new ReporteCompras.Resumen(activas, totalComprado, anuladas, totalAnulado);
        if (activas == 0) {
            return new ReporteCompras(depositoVisible, resumen, porDia, List.of(), List.of(), List.of());
        }

        var proveedores = repository.comprasPorProveedor(desde, hasta, proveedorId, depositoVisible).stream()
                .map(fila -> new ReporteCompras.Grupo(fila.getId(),
                        nombre(fila.getNombre(), "Proveedor", fila.getId()), fila.getFacturas(), fila.getTotal()))
                .toList();
        var mediosPago = repository.comprasPorMedioPago(desde, hasta, proveedorId, depositoVisible).stream()
                .map(fila -> new ReporteCompras.Grupo(fila.getId(), fila.getNombre(), fila.getFacturas(), fila.getTotal()))
                .toList();
        var productos = repository.productosComprados(desde, hasta, proveedorId, depositoVisible).stream()
                .map(fila -> new ReporteCompras.Producto(fila.getId(),
                        nombre(fila.getNombre(), "Producto", fila.getId()), fila.getFacturas(),
                        fila.getUnidades(), fila.getTotal()))
                .toList();
        return new ReporteCompras(depositoVisible, resumen, porDia, proveedores, mediosPago, productos);
    }

    private String nombre(String nombre, String tipo, Long id) {
        return nombre != null ? nombre : tipo + " #" + id;
    }
}
