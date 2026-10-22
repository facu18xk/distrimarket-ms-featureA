package com.distrimarket.compras.config;

import com.distrimarket.commons.security.JwtDecoderConfig;
import com.distrimarket.commons.security.ResourceServerSecurityConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Activa la seguridad JWT compartida. Las rutas públicas y los roles se configuran
 * en application.yml (bloque {@code security}).
 */
@Configuration
@Import({JwtDecoderConfig.class, ResourceServerSecurityConfig.class})
public class SecurityConfig {
}
