package com.distrimarket.compras.controller;

import com.distrimarket.commons.controller.BaseController;
import com.distrimarket.commons.dto.ProveedorProductoDTO;
import com.distrimarket.compras.service.ProveedorProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compras/proveedores-productos")
public class ProveedorProductoController extends BaseController<ProveedorProductoDTO, ProveedorProductoDTO, Long> {

    private final ProveedorProductoService proveedorProductoService;

    public ProveedorProductoController(ProveedorProductoService proveedorProductoService) {
        super(proveedorProductoService);
        this.proveedorProductoService = proveedorProductoService;
    }

    @GetMapping("/proveedor/{proveedorId}")
    public ResponseEntity<List<ProveedorProductoDTO>> listarPorProveedor(
            @PathVariable("proveedorId") Long proveedorId) {
        List<ProveedorProductoDTO> lista = proveedorProductoService.listarPorProveedor(proveedorId);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/proveedor/{proveedorId}/producto/{productoId}")
    public ResponseEntity<ProveedorProductoDTO> obtenerPorProveedorYProducto(
            @PathVariable("proveedorId") Long proveedorId,
            @PathVariable("productoId") Long productoId) {
        return proveedorProductoService.obtenerPorProveedorYProducto(proveedorId, productoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
