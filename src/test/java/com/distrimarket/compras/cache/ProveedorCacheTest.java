package com.distrimarket.compras.cache;

import com.distrimarket.commons.dto.ProveedorResponseDTO;
import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.compras.config.cache.CacheDomains;
import com.distrimarket.compras.config.cache.DomainCache;
import com.distrimarket.compras.mapper.ProveedorMapper;
import com.distrimarket.compras.repository.PersonaRepository;
import com.distrimarket.compras.repository.ProveedorRepository;
import com.distrimarket.compras.service.ProveedorService;
import com.distrimarket.compras.service.impl.ProveedorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(ProveedorCacheTest.CacheTestConfig.class)
class ProveedorCacheTest {

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private ProveedorMapper proveedorMapper;

    @BeforeEach
    void prepararProveedor() {
        Proveedor proveedor = Proveedor.builder()
                .id(1L)
                .nombreFantasia("Proveedor de prueba")
                .estado(true)
                .build();
        ProveedorResponseDTO dto = new ProveedorResponseDTO();
        dto.setIdProveedor(1L);
        dto.setNombreFantasia("Proveedor de prueba");
        dto.setEstado(true);

        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(proveedorMapper.toDto(proveedor)).thenReturn(dto);
    }

    @Test
    void laSegundaLecturaSaleDeLaCache() {
        proveedorService.obtenerPorId(1L);
        proveedorService.obtenerPorId(1L);

        verify(proveedorRepository, times(1)).findById(1L);
    }

    @Configuration
    @EnableCaching
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheDomains.PROVEEDOR);
        }

        @Bean
        DomainCache domainCache(CacheManager cacheManager) {
            return new DomainCache(cacheManager);
        }

        @Bean
        ProveedorRepository proveedorRepository() {
            return mock(ProveedorRepository.class);
        }

        @Bean
        PersonaRepository personaRepository() {
            return mock(PersonaRepository.class);
        }

        @Bean
        ProveedorMapper proveedorMapper() {
            return mock(ProveedorMapper.class);
        }

        @Bean
        ProveedorServiceImpl proveedorService(ProveedorRepository proveedorRepository,
                                              ProveedorMapper proveedorMapper,
                                              PersonaRepository personaRepository,
                                              DomainCache domainCache) {
            return new ProveedorServiceImpl(proveedorRepository, proveedorMapper,
                    personaRepository, domainCache);
        }
    }
}
