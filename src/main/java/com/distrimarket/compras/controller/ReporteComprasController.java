package com.distrimarket.compras.controller;

import com.distrimarket.commons.dto.ErrorResponseDTO;
import com.distrimarket.compras.service.ReporteCompras;
import com.distrimarket.compras.service.ReporteComprasService;
import com.distrimarket.compras.service.ReporteComprasService.Estado;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/compras/facturas/reporte")
public class ReporteComprasController {

    private final ReporteComprasService service;

    public ReporteComprasController(ReporteComprasService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Totales y rankings de compras por período")
    public ReporteCompras generar(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "proveedorId", required = false) Long proveedorId,
            @RequestParam(name = "depositoId", required = false) Long depositoId,
            @RequestParam(name = "estado", required = false) Estado estado) {
        return service.generar(desde, hasta, proveedorId, depositoId, estado);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO filtroFaltante(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return errorFiltro(ex.getParameterName(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDTO filtroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return errorFiltro(ex.getName(), request);
    }

    private ErrorResponseDTO errorFiltro(String campo, HttpServletRequest request) {
        return new ErrorResponseDTO(OffsetDateTime.now(), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "El filtro '" + campo + "' es obligatorio o tiene un valor inválido.", request.getRequestURI());
    }
}
