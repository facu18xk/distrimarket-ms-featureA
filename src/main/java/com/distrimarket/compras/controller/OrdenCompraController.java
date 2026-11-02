package com.distrimarket.compras.controller;

import com.distrimarket.commons.dto.OrdenCompraDTO;
import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.service.Paginas;
import com.distrimarket.compras.service.OrdenCompraService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/compras/ordenes")
public class OrdenCompraController {

    private static final Set<String> ORDENES_PERMITIDOS = Set.of(
            "id", "fechaCreacion", "estado", "total");
    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Direction.ASC, "id");

    private final OrdenCompraService ordenCompraService;

    public OrdenCompraController(OrdenCompraService ordenCompraService) {
        this.ordenCompraService = ordenCompraService;
    }

    @PostMapping
    public ResponseEntity<OrdenCompraDTO> crear(@Valid @RequestBody OrdenCompraDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenCompraService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdenCompraDTO> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody OrdenCompraDTO dto) {
        return ResponseEntity.ok(ordenCompraService.actualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompraDTO> obtenerPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ordenCompraService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<OrdenCompraDTO>> listar(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "proveedorId", required = false) Long proveedorId,
            @RequestParam(name = "estado", required = false) String estado,
            Pageable pageable) {
        Pageable paginacion = normalizarOrden(pageable);
        return ResponseEntity.ok(
                ordenCompraService.buscarPaginado(q, proveedorId, estado, paginacion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        ordenCompraService.eliminarLogico(id);
        return ResponseEntity.noContent().build();
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

    private Pageable normalizarOrden(Pageable pageable) {
        return Paginas.conOrdenPermitido(pageable, ORDENES_PERMITIDOS, ORDEN_POR_DEFECTO);
    }
}
