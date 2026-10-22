package com.distrimarket.compras.controller;

import com.distrimarket.commons.controller.BaseController;
import com.distrimarket.commons.dto.PageResponseDTO;
import com.distrimarket.commons.dto.ProveedorRequestDTO;
import com.distrimarket.commons.dto.ProveedorResponseDTO;
import com.distrimarket.compras.service.ProveedorService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/compras/proveedores")
public class ProveedorController extends BaseController<ProveedorRequestDTO, ProveedorResponseDTO, Long> {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        super(proveedorService);
        this.proveedorService = proveedorService;
    }

    @Override
    @GetMapping
    public ResponseEntity<PageResponseDTO<ProveedorResponseDTO>> listarPaginado(Pageable pageable) {
        Sort sort = Sort.by(pageable.getSort().stream()
                .map(order -> "fechaEmision".equals(order.getProperty())
                        ? order.withProperty("fechaCreacion")
                        : order)
                .collect(Collectors.toList()));

        Pageable paginacion = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return ResponseEntity.ok(proveedorService.listarPaginado(paginacion));
    }

    @GetMapping("/ruc/{ruc}")
    public ResponseEntity<ProveedorResponseDTO> obtenerPorRuc(@PathVariable("ruc") String ruc) {
        return proveedorService.obtenerPorRuc(ruc)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
