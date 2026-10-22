package com.distrimarket.compras.cache;

import com.distrimarket.commons.dto.FacturaCompraResponseDTO;
import com.distrimarket.commons.dto.ProveedorResponseDTO;
import com.distrimarket.compras.config.cache.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;

class CacheSerializationTest {

    private final RedisSerializer<Object> serializer = CacheConfig.valueSerializer();

    @SuppressWarnings("unchecked")
    private <T> T roundTrip(T value) {
        return (T) serializer.deserialize(serializer.serialize(value));
    }

    @Test
    void unProveedorConservaSuTipoYSuId() {
        ProveedorResponseDTO dto = new ProveedorResponseDTO();
        dto.setIdProveedor(5L);
        dto.setNombreFantasia("Distribuidora del Este");
        dto.setEstado(true);

        ProveedorResponseDTO leido = roundTrip(dto);

        assertThat(leido).isInstanceOf(ProveedorResponseDTO.class).isEqualTo(dto);
    }

    @Test
    void unaFacturaConservaSuTipoYSuId() {
        FacturaCompraResponseDTO dto = new FacturaCompraResponseDTO();
        dto.setIdFacturaCompra(12L);
        dto.setNumeroFactura("001-001-0000123");
        dto.setEstado("ACTIVA");

        FacturaCompraResponseDTO leido = roundTrip(dto);

        assertThat(leido).isInstanceOf(FacturaCompraResponseDTO.class).isEqualTo(dto);
    }
}
