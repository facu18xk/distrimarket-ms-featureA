package com.distrimarket.compras.security;

import com.distrimarket.commons.security.JwtDecoderConfig;
import com.distrimarket.commons.security.ResourceServerSecurityConfig;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SecurityRulesTest.TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "security.enabled=true",
        "security.jwt.secret=test-secret-with-at-least-32-characters!!",
        "security.jwt.issuer=distrimarket-auth",
        "security.public-paths=/v3/api-docs/**,/swagger-ui/**,/swagger-ui.html,/actuator/health/**,/actuator/info,/error",
        "security.rules[0].pattern=/**",
        "security.rules[0].roles=ADMIN,COMPRADOR"
})
class SecurityRulesTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void withoutTokenReturns401() throws Exception {
        mvc.perform(get("/compras/facturas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRoleReturns403() throws Exception {
        mvc.perform(get("/compras/facturas").header("Authorization", token("VENDEDOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void compradorCanList() throws Exception {
        mvc.perform(get("/compras/facturas").header("Authorization", token("COMPRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void adminCanList() throws Exception {
        mvc.perform(get("/compras/facturas").header("Authorization", token("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    private String token(String... roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("distrimarket-auth")
                .subject("test-user")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of(roles))
                .build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            LiquibaseAutoConfiguration.class
    })
    @Import({JwtDecoderConfig.class, ResourceServerSecurityConfig.class, TokenConfig.class, TestController.class})
    static class TestApplication {
    }

    @RestController
    static class TestController {

        @GetMapping("/compras/facturas")
        String list() {
            return "ok";
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TokenConfig {

        @Bean
        JwtEncoder jwtEncoder(@Value("${security.jwt.secret}") String secret) {
            SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            return new NimbusJwtEncoder(new ImmutableSecret<>(key));
        }
    }
}
