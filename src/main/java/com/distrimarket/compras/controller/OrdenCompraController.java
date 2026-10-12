package com.distrimarket.compras.controller;

import com.distrimarket.commons.controller.BaseController;
import com.distrimarket.commons.dto.OrdenCompraDTO;
import com.distrimarket.compras.service.OrdenCompraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compras/ordenes")
public class OrdenCompraController extends BaseController<OrdenCompraDTO, OrdenCompraDTO, Long> {

    private final OrdenCompraService ordenCompraService;

    public OrdenCompraController(OrdenCompraService ordenCompraService) {
        super(ordenCompraService);
        this.ordenCompraService = ordenCompraService;
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<OrdenCompraDTO> cambiarEstado(
            @PathVariable("id") Long id,
            @RequestParam("estado") String estado) {
        OrdenCompraDTO actualizada = ordenCompraService.cambiarEstado(id, estado);
        return ResponseEntity.ok(actualizada);
    }

    @GetMapping("/proveedor/{proveedorId}")
    public ResponseEntity<List<OrdenCompraDTO>> listarPorProveedor(
            @PathVariable("proveedorId") Long proveedorId) {
        List<OrdenCompraDTO> lista = ordenCompraService.listarPorProveedor(proveedorId);
        return ResponseEntity.ok(lista);
    }
}
