package com.distrimarket.compras.service.impl;

import com.distrimarket.commons.dto.FacturaCompraRequestDTO;
import com.distrimarket.commons.dto.OrdenCompraDTO;
import com.distrimarket.commons.entity.Deposito;
import com.distrimarket.commons.entity.FacturaCompra;
import com.distrimarket.commons.entity.MedioPago;
import com.distrimarket.commons.entity.OrdenCompra;
import com.distrimarket.commons.entity.Proveedor;
import com.distrimarket.compras.controller.OrdenCompraController;
import com.distrimarket.compras.repository.DepositoRepository;
import com.distrimarket.compras.repository.EmpleadoRepository;
import com.distrimarket.compras.repository.FacturaCompraRepository;
import com.distrimarket.compras.repository.MedioPagoRepository;
import com.distrimarket.compras.repository.OrdenCompraRepository;
import com.distrimarket.compras.repository.PersonaRepository;
import com.distrimarket.compras.repository.ProductoRepository;
import com.distrimarket.compras.repository.ProveedorProductoRepository;
import com.distrimarket.compras.repository.ProveedorRepository;
import com.distrimarket.compras.repository.StockDepositoRepository;
import com.distrimarket.compras.service.FacturaCompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Prueba de integración que ejecuta los 7 modos de propagación de Spring (@Transactional)
 * usando JpaTransactionManager real, tanto en llamada directa como desde otro servicio.
 * Se habilita cuando DB_URL está definido (por ejemplo desde .env.local).
 */
@SpringBootTest(properties = {
        "distrimarket.cache.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "spring.liquibase.enabled=false",
        "logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG",
        "logging.level.org.springframework.transaction.interceptor=TRACE"
})
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class TransaccionPropagacionIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(TransaccionPropagacionIntegrationTest.class);

    @Autowired
    private OrdenCompraController ordenCompraController;

    @Autowired
    private FacturaCompraService facturaCompraService;

    @Autowired
    private OrdenCompraPropagacionService targetService;

    @Autowired
    private FacturaCompraCallerService callerService;

    @MockBean private OrdenCompraRepository ordenCompraRepository;
    @MockBean private FacturaCompraRepository facturaCompraRepository;
    @MockBean private ProveedorRepository proveedorRepository;
    @MockBean private DepositoRepository depositoRepository;
    @MockBean private MedioPagoRepository medioPagoRepository;
    @MockBean private ProductoRepository productoRepository;
    @MockBean private StockDepositoRepository stockDepositoRepository;
    @MockBean private EmpleadoRepository empleadoRepository;
    @MockBean private PersonaRepository personaRepository;
    @MockBean private ProveedorProductoRepository proveedorProductoRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        OrdenCompra orden = OrdenCompra.builder()
                .id(10L)
                .estado("PENDIENTE")
                .total(BigDecimal.valueOf(1000))
                .build();
        when(ordenCompraRepository.findById(10L)).thenReturn(Optional.of(orden));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(inv -> inv.getArgument(0));

        Proveedor proveedor = Proveedor.builder().id(1L).nombreFantasia("Proveedor Demo").build();
        Deposito deposito = Deposito.builder().id(1L).nombre("Deposito Central").build();
        MedioPago medioPago = MedioPago.builder().id(1L).nombre("Efectivo").build();

        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(depositoRepository.findById(1L)).thenReturn(Optional.of(deposito));
        when(medioPagoRepository.findById(1L)).thenReturn(Optional.of(medioPago));
        when(facturaCompraRepository.existsByProveedorIdAndNumeroFactura(1L, "001-001-0000123")).thenReturn(false);
        when(facturaCompraRepository.save(any(FacturaCompra.class))).thenAnswer(inv -> {
            FacturaCompra fc = inv.getArgument(0);
            fc.setId(99L);
            return fc;
        });
    }

    @Test
    void demostrarFlujoRealYCombinacionesDePropagacion() {
        log.info("==================== 0. FLUJO REAL DEL PROYECTO ====================");
        log.info("--- 0A. Llamada directa: OrdenCompraController.cambiarEstado ---");
        var respDirecta = ordenCompraController.cambiarEstado(10L, "APROBADA");
        assertThat(respDirecta.getBody()).isNotNull();

        log.info("--- 0B. Llamada desde otro servicio: FacturaCompraService.crear -> OrdenCompraService.cambiarEstado ---");
        FacturaCompraRequestDTO facturaDto = new FacturaCompraRequestDTO();
        facturaDto.setIdProveedor(1L);
        facturaDto.setIdDeposito(1L);
        facturaDto.setIdMedioPago(1L);
        facturaDto.setIdOrdenCompra(10L);
        facturaDto.setNumeroFactura("001-001-0000123");
        facturaDto.setFechaEmision(LocalDate.of(2026, 11, 12));
        var facturaCreada = facturaCompraService.crear(facturaDto);
        assertThat(facturaCreada).isNotNull();

        log.info("==================== 1. PROPAGATION.REQUIRED ====================");
        log.info("--- 1A. Llamada directa (REQUIRED) ---");
        assertThat(targetService.cambiarEstadoRequired(10L, "REQUIRED_DIRECTA")).isTrue();
        log.info("--- 1B. Llamada desde otro servicio (REQUIRED) ---");
        assertThat(callerService.invocarRequired(10L)).isTrue();

        log.info("==================== 2. PROPAGATION.REQUIRES_NEW ====================");
        log.info("--- 2A. Llamada directa (REQUIRES_NEW) ---");
        assertThat(targetService.cambiarEstadoRequiresNew(10L, "REQUIRES_NEW_DIRECTA")).isTrue();
        log.info("--- 2B. Llamada desde otro servicio (REQUIRES_NEW) ---");
        assertThat(callerService.invocarRequiresNew(10L)).isTrue();

        log.info("==================== 3. PROPAGATION.SUPPORTS ====================");
        log.info("--- 3A. Llamada directa (SUPPORTS) ---");
        assertThat(targetService.cambiarEstadoSupports(10L, "SUPPORTS_DIRECTA")).isFalse();
        log.info("--- 3B. Llamada desde otro servicio (SUPPORTS) ---");
        assertThat(callerService.invocarSupports(10L)).isTrue();

        log.info("==================== 4. PROPAGATION.NOT_SUPPORTED ====================");
        log.info("--- 4A. Llamada directa (NOT_SUPPORTED) ---");
        assertThat(targetService.cambiarEstadoNotSupported(10L, "NOT_SUPPORTED_DIRECTA")).isFalse();
        log.info("--- 4B. Llamada desde otro servicio (NOT_SUPPORTED) ---");
        assertThat(callerService.invocarNotSupported(10L)).isFalse();

        log.info("==================== 5. PROPAGATION.MANDATORY ====================");
        log.info("--- 5A. Llamada directa (MANDATORY - debe lanzar excepcion) ---");
        assertThatThrownBy(() -> targetService.cambiarEstadoMandatory(10L, "MANDATORY_DIRECTA"))
                .isInstanceOf(IllegalTransactionStateException.class);
        log.info("--- 5B. Llamada desde otro servicio (MANDATORY) ---");
        assertThat(callerService.invocarMandatory(10L)).isTrue();

        log.info("==================== 6. PROPAGATION.NEVER ====================");
        log.info("--- 6A. Llamada directa (NEVER) ---");
        assertThat(targetService.cambiarEstadoNever(10L, "NEVER_DIRECTA")).isFalse();
        log.info("--- 6B. Llamada desde otro servicio (NEVER - debe lanzar excepcion) ---");
        assertThatThrownBy(() -> callerService.invocarNever(10L))
                .isInstanceOf(IllegalTransactionStateException.class);

        log.info("==================== 7. PROPAGATION.NESTED ====================");
        log.info("--- 7A. Llamada directa (NESTED) ---");
        assertThat(targetService.cambiarEstadoNested(10L, "NESTED_DIRECTA")).isTrue();
        log.info("--- 7B. Llamada desde otro servicio (NESTED - JpaDialect lanza excepcion) ---");
        assertThatThrownBy(() -> callerService.invocarNested(10L))
                .isInstanceOf(NestedTransactionNotSupportedException.class);
    }

    @TestConfiguration
    static class PropagacionTestConfig {

        @Bean
        OrdenCompraPropagacionService ordenCompraPropagacionService(OrdenCompraRepository ordenCompraRepository) {
            return new OrdenCompraPropagacionService(ordenCompraRepository);
        }

        @Bean
        FacturaCompraCallerService facturaCompraCallerService(OrdenCompraPropagacionService target) {
            return new FacturaCompraCallerService(target);
        }
    }

    @Service
    static class OrdenCompraPropagacionService {

        private static final Logger log = LoggerFactory.getLogger(OrdenCompraPropagacionService.class);
        private final OrdenCompraRepository ordenCompraRepository;

        OrdenCompraPropagacionService(OrdenCompraRepository ordenCompraRepository) {
            this.ordenCompraRepository = ordenCompraRepository;
        }

        private boolean ejecutarCambioEstado(String propagacion, Long id, String estado) {
            boolean activa = TransactionSynchronizationManager.isActualTransactionActive();
            String nombre = TransactionSynchronizationManager.getCurrentTransactionName();
            log.info("[TARGET - {}] txActiva={} | txNombre={} | ordenId={} -> {}",
                    propagacion, activa, nombre, id, estado);
            OrdenCompra oc = ordenCompraRepository.findById(id).orElseThrow();
            oc.setEstado(estado);
            ordenCompraRepository.save(oc);
            return activa;
        }

        @Transactional(propagation = Propagation.REQUIRED)
        public boolean cambiarEstadoRequired(Long id, String estado) {
            return ejecutarCambioEstado("REQUIRED", id, estado);
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public boolean cambiarEstadoRequiresNew(Long id, String estado) {
            return ejecutarCambioEstado("REQUIRES_NEW", id, estado);
        }

        @Transactional(propagation = Propagation.SUPPORTS)
        public boolean cambiarEstadoSupports(Long id, String estado) {
            return ejecutarCambioEstado("SUPPORTS", id, estado);
        }

        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        public boolean cambiarEstadoNotSupported(Long id, String estado) {
            return ejecutarCambioEstado("NOT_SUPPORTED", id, estado);
        }

        @Transactional(propagation = Propagation.MANDATORY)
        public boolean cambiarEstadoMandatory(Long id, String estado) {
            return ejecutarCambioEstado("MANDATORY", id, estado);
        }

        @Transactional(propagation = Propagation.NEVER)
        public boolean cambiarEstadoNever(Long id, String estado) {
            return ejecutarCambioEstado("NEVER", id, estado);
        }

        @Transactional(propagation = Propagation.NESTED)
        public boolean cambiarEstadoNested(Long id, String estado) {
            return ejecutarCambioEstado("NESTED", id, estado);
        }
    }

    @Service
    static class FacturaCompraCallerService {

        private static final Logger log = LoggerFactory.getLogger(FacturaCompraCallerService.class);
        private final OrdenCompraPropagacionService target;

        FacturaCompraCallerService(OrdenCompraPropagacionService target) {
            this.target = target;
        }

        private void logOuter(String caso) {
            log.info("[CALLER FacturaCompraService - {}] txActiva={} | txNombre={}",
                    caso,
                    TransactionSynchronizationManager.isActualTransactionActive(),
                    TransactionSynchronizationManager.getCurrentTransactionName());
        }

        @Transactional
        public boolean invocarRequired(Long id) {
            logOuter("REQUIRED");
            return target.cambiarEstadoRequired(id, "FACTURADA");
        }

        @Transactional
        public boolean invocarRequiresNew(Long id) {
            logOuter("REQUIRES_NEW");
            return target.cambiarEstadoRequiresNew(id, "FACTURADA");
        }

        @Transactional
        public boolean invocarSupports(Long id) {
            logOuter("SUPPORTS");
            return target.cambiarEstadoSupports(id, "FACTURADA");
        }

        @Transactional
        public boolean invocarNotSupported(Long id) {
            logOuter("NOT_SUPPORTED");
            return target.cambiarEstadoNotSupported(id, "FACTURADA");
        }

        @Transactional
        public boolean invocarMandatory(Long id) {
            logOuter("MANDATORY");
            return target.cambiarEstadoMandatory(id, "FACTURADA");
        }

        @Transactional
        public boolean invocarNever(Long id) {
            logOuter("NEVER");
            return target.cambiarEstadoNever(id, "FACTURADA");
        }

        @Transactional
        public boolean invocarNested(Long id) {
            logOuter("NESTED");
            return target.cambiarEstadoNested(id, "FACTURADA");
        }
    }
}
