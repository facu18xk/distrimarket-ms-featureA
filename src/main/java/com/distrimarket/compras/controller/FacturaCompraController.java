package com.distrimarket.compras.controller;

import com.distrimarket.commons.controller.BaseController;
import com.distrimarket.commons.dto.FacturaCompraRequestDTO;
import com.distrimarket.commons.dto.FacturaCompraResponseDTO;
import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.compras.service.FacturaCompraService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/compras/facturas")
public class FacturaCompraController extends BaseController<FacturaCompraRequestDTO, FacturaCompraResponseDTO, Long> {

    private final FacturaCompraService facturaCompraService;

    public FacturaCompraController(FacturaCompraService facturaCompraService) {
        super(facturaCompraService);
        this.facturaCompraService = facturaCompraService;
    }

    @PatchMapping("/{id}/anular")
    public ResponseEntity<FacturaCompraResponseDTO> anular(@PathVariable("id") Long id) {
        FacturaCompraResponseDTO anulada = facturaCompraService.anularFactura(id);
        return ResponseEntity.ok(anulada);
    }

    @GetMapping("/proveedor/{proveedorId}")
    public ResponseEntity<List<FacturaCompraResponseDTO>> listarPorProveedor(
            @PathVariable("proveedorId") Long proveedorId) {
        List<FacturaCompraResponseDTO> lista = facturaCompraService.listarPorProveedor(proveedorId);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/buscar")
    public ResponseEntity<PageResponseDTO<FacturaCompraResponseDTO>> buscar(
            @RequestParam(name = "proveedorId", required = false) Long proveedorId,
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "numeroFactura", required = false) String numeroFactura,
            Pageable pageable) {
        return ResponseEntity.ok(
                facturaCompraService.buscarPaginado(proveedorId, estado, desde, hasta, numeroFactura, pageable));
    }
}
