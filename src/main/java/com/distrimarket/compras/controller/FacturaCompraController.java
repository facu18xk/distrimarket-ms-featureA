package com.distrimarket.compras.controller;

import com.distrimarket.commons.dto.FacturaCompraRequestDTO;
import com.distrimarket.commons.dto.FacturaCompraResponseDTO;
import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.service.Paginas;
import com.distrimarket.compras.service.FacturaCompraService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/compras/facturas")
public class FacturaCompraController {

    private static final Set<String> ORDENES_PERMITIDOS = Set.of(
            "id", "numeroFactura", "fechaEmision", "condicionCompra",
            "totalGeneral", "estado", "fechaCreacion");
    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Direction.ASC, "id");

    private final FacturaCompraService facturaCompraService;

    public FacturaCompraController(FacturaCompraService facturaCompraService) {
        this.facturaCompraService = facturaCompraService;
    }

    @PostMapping
    public ResponseEntity<FacturaCompraResponseDTO> crear(@Valid @RequestBody FacturaCompraRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaCompraService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacturaCompraResponseDTO> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody FacturaCompraRequestDTO dto) {
        return ResponseEntity.ok(facturaCompraService.actualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacturaCompraResponseDTO> obtenerPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(facturaCompraService.obtenerPorId(id));
    }

    /**
     * Listado raíz: conserva la búsqueda libre {@code q} y agrega los filtros estructurados
     * de proveedor, estado, número y rango de fechas de emisión.
     */
    @GetMapping
    public ResponseEntity<PageResponseDTO<FacturaCompraResponseDTO>> listar(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "proveedorId", required = false) Long proveedorId,
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "numeroFactura", required = false) String numeroFactura,
            Pageable pageable) {
        Pageable paginacion = normalizarOrden(pageable);
        return ResponseEntity.ok(facturaCompraService.buscarPaginado(
                q, proveedorId, estado, desde, hasta, numeroFactura, paginacion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        facturaCompraService.eliminarLogico(id);
        return ResponseEntity.noContent().build();
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
        Pageable paginacion = normalizarOrden(pageable);
        return ResponseEntity.ok(
                facturaCompraService.buscarPaginado(proveedorId, estado, desde, hasta, numeroFactura, paginacion));
    }

    private Pageable normalizarOrden(Pageable pageable) {
        return Paginas.conOrdenPermitido(pageable, ORDENES_PERMITIDOS, ORDEN_POR_DEFECTO);
    }
}
