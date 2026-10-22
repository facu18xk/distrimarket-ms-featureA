package com.distrimarket.compras.cache;

import com.distrimarket.commons.dto.ProveedorResponseDTO;
import com.distrimarket.compras.config.cache.CacheDomains;
import com.distrimarket.compras.config.cache.DomainCache;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainCacheTest {

    private final CacheManager cacheManager = new ConcurrentMapCacheManager(CacheDomains.PROVEEDOR);
    private final DomainCache domainCache = new DomainCache(cacheManager);

    @Test
    void putAllGuardaCadaElementoPorSuId() {
        ProveedorResponseDTO dto = new ProveedorResponseDTO();
        dto.setIdProveedor(7L);

        domainCache.putAll(CacheDomains.PROVEEDOR, List.of(dto), ProveedorResponseDTO::getIdProveedor);

        assertThat(cacheManager.getCache(CacheDomains.PROVEEDOR).get(7L).get()).isEqualTo(dto);
    }

    @Test
    void evictQuitaElElemento() {
        ProveedorResponseDTO dto = new ProveedorResponseDTO();
        dto.setIdProveedor(7L);
        domainCache.put(CacheDomains.PROVEEDOR, 7L, dto);

        domainCache.evict(CacheDomains.PROVEEDOR, 7L);

        assertThat(cacheManager.getCache(CacheDomains.PROVEEDOR).get(7L)).isNull();
    }
}
