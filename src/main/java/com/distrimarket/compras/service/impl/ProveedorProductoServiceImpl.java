package com.distrimarket.compras.service.impl;

import com.distrimarket.commons.dto.ProveedorProductoDTO;
import com.distrimarket.commons.entity.Producto;
import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.commons.entity.ProveedorProducto;
import com.distrimarket.commons.exception.ResourceNotFoundException;
import com.distrimarket.commons.service.impl.BaseServiceImpl;
import com.distrimarket.compras.mapper.ProveedorProductoMapper;
import com.distrimarket.compras.repository.ProductoRepository;
import com.distrimarket.compras.repository.ProveedorProductoRepository;
import com.distrimarket.compras.repository.ProveedorRepository;
import com.distrimarket.compras.service.ProveedorProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ProveedorProductoServiceImpl extends BaseServiceImpl<ProveedorProducto, Long, ProveedorProductoDTO, ProveedorProductoDTO> implements ProveedorProductoService {

    private final ProveedorProductoRepository proveedorProductoRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    public ProveedorProductoServiceImpl(
            ProveedorProductoRepository proveedorProductoRepository,
            ProveedorProductoMapper proveedorProductoMapper,
            ProveedorRepository proveedorRepository,
            ProductoRepository productoRepository) {
        super(proveedorProductoRepository, proveedorProductoMapper);
        this.proveedorProductoRepository = proveedorProductoRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public ProveedorProductoDTO crear(ProveedorProductoDTO dto) {
        log.info("Registrando acuerdo comercial proveedor-producto: Prov ID {}, Prod ID {}",
                dto.getIdProveedor(), dto.getIdProducto());

        Proveedor proveedor = proveedorRepository.findById(dto.getIdProveedor())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", "id", dto.getIdProveedor()));

        Producto producto = productoRepository.findById(dto.getIdProducto())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", dto.getIdProducto()));

        BigDecimal costoAcordado = dto.getPrecioCostoAcordado() != null
                ? BigDecimal.valueOf(dto.getPrecioCostoAcordado())
                : producto.getPrecioCosto();

        // Si ya existe acuerdo entre el proveedor y producto, actualizamos
        ProveedorProducto entidad = proveedorProductoRepository
                .findByProveedorIdAndProductoId(dto.getIdProveedor(), dto.getIdProducto())
                .orElseGet(() -> ProveedorProducto.builder()
                        .proveedor(proveedor)
                        .producto(producto)
                        .build());

        entidad.setPrecioCostoAcordado(costoAcordado);
        entidad.setCodigoProveedor(dto.getCodigoProveedor());
        entidad.setActivo(dto.getActivo() != null ? dto.getActivo() : true);

        ProveedorProducto guardado = proveedorProductoRepository.save(entidad);
        log.info("Acuerdo comercial guardado con éxito. ID: {}", guardado.getId());

        return mapper.toDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorProductoDTO> listarPorProveedor(Long proveedorId) {
        return proveedorProductoRepository.findByProveedorId(proveedorId).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProveedorProductoDTO> obtenerPorProveedorYProducto(Long proveedorId, Long productoId) {
        return proveedorProductoRepository.findByProveedorIdAndProductoId(proveedorId, productoId)
                .map(mapper::toDto);
    }
}
