package com.distrimarket.compras.controller;

import com.distrimarket.commons.controller.BaseController;
import com.distrimarket.commons.dto.FacturaCompraRequestDTO;
import com.distrimarket.commons.dto.FacturaCompraResponseDTO;
import com.distrimarket.compras.service.FacturaCompraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
