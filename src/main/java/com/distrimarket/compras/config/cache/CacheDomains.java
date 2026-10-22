package com.distrimarket.compras.config.cache;

/**
 * Nombres de las secciones de caché de Compras. Deben coincidir con las claves de
 * {@code distrimarket.cache.domains} en application.yml.
 */
public final class CacheDomains {

    public static final String PROVEEDOR = "proveedor";
    public static final String FACTURA_COMPRA = "facturaCompra";
    public static final String ORDEN_COMPRA = "ordenCompra";

    private CacheDomains() {
    }
}
