package com.distrimarket.compras.controller;

import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.dto.ProveedorRequestDTO;
import com.distrimarket.commons.dto.ProveedorResponseDTO;
import com.distrimarket.commons.service.Paginas;
import com.distrimarket.compras.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/compras/proveedores")
public class ProveedorController {

    private static final Set<String> ORDENES_PERMITIDOS = Set.of(
            "id", "nombreFantasia", "estado", "fechaCreacion");
    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Direction.ASC, "id");

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @PostMapping
    public ResponseEntity<ProveedorResponseDTO> crear(@Valid @RequestBody ProveedorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponseDTO> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProveedorRequestDTO dto) {
        return ResponseEntity.ok(proveedorService.actualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponseDTO> obtenerPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(proveedorService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<ProveedorResponseDTO>> listarPaginado(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "estado", required = false) Boolean estado,
            Pageable pageable) {
        Pageable paginacion = normalizarOrden(pageable);
        return ResponseEntity.ok(proveedorService.buscarPaginado(q, estado, paginacion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        proveedorService.eliminarLogico(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ruc/{ruc}")
    public ResponseEntity<ProveedorResponseDTO> obtenerPorRuc(@PathVariable("ruc") String ruc) {
        return proveedorService.obtenerPorRuc(ruc)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Pageable normalizarOrden(Pageable pageable) {
        if (pageable == null) {
            return null;
        }
        // Compatibilidad con el frontend: fechaEmision es un alias de fechaCreacion.
        Sort sort = Sort.by(pageable.getSort().stream()
                .map(order -> "fechaEmision".equals(order.getProperty())
                        ? order.withProperty("fechaCreacion")
                        : order)
                .toList());
        Pageable conAlias = pageable.isUnpaged()
                ? Pageable.unpaged(sort)
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return Paginas.conOrdenPermitido(conAlias, ORDENES_PERMITIDOS, ORDEN_POR_DEFECTO);
    }
}
