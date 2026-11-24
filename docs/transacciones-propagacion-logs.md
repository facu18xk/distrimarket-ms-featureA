# Propagación de Transacciones en Spring (`@Transactional`) — Evidencia de Logs

Microservicio: **`distrimarket-ms-compras`** (Spring Boot 3.2.4, Spring Data JPA, `JpaTransactionManager`, PostgreSQL).

## 1. Implementación en el Proyecto

- **Servicio destino (`OrdenCompraServiceImpl.cambiarEstado`):** anotado con `@Transactional(propagation = Propagation.REQUIRED)` en [OrdenCompraServiceImpl.java](file:///home/facu/IdeaProjects/distrimarket-ms-featureA/src/main/java/com/distrimarket/compras/service/impl/OrdenCompraServiceImpl.java).
  - **Llamada directa:** `OrdenCompraController.cambiarEstado(...)` (`PATCH /compras/ordenes/{id}/estado`) invoca directamente al servicio sin transacción previa.
- **Llamada desde otro servicio (`FacturaCompraServiceImpl.crear` → `OrdenCompraServiceImpl.cambiarEstado`):** en [FacturaCompraServiceImpl.java](file:///home/facu/IdeaProjects/distrimarket-ms-featureA/src/main/java/com/distrimarket/compras/service/impl/FacturaCompraServiceImpl.java), `crear(...)` (anotado con `@Transactional`) inyecta `OrdenCompraService` y, cuando la factura referencia una orden de compra (`dto.getIdOrdenCompra() != null`), actualiza su estado a `"FACTURADA"`.
- **Prueba de integración:** [TransaccionPropagacionIntegrationTest.java](file:///home/facu/IdeaProjects/distrimarket-ms-featureA/src/test/java/com/distrimarket/compras/service/impl/TransaccionPropagacionIntegrationTest.java) ejecuta el flujo real y todas las combinaciones de `Propagation` con `JpaTransactionManager` en nivel `DEBUG` y `TransactionInterceptor` en nivel `TRACE`.

---

## 2. Resumen de Comportamiento

| Propagación | Llamada directa (sin TX previa) | Llamada desde otro servicio (con TX activa) |
| :--- | :--- | :--- |
| `REQUIRED` | Crea una nueva TX | Participa en la TX existente (`txNombre` = método caller) |
| `REQUIRES_NEW` | Crea una nueva TX | Suspende la TX actual, crea una nueva TX independiente y luego reanuda la original |
| `SUPPORTS` | Se ejecuta **sin** TX (`txActiva=false`) | Participa en la TX existente |
| `NOT_SUPPORTED` | Se ejecuta **sin** TX (`txActiva=false`) | Suspende la TX actual, ejecuta sin TX y luego reanuda la original |
| `MANDATORY` | Lanza `IllegalTransactionStateException` | Participa en la TX existente |
| `NEVER` | Se ejecuta **sin** TX (`txActiva=false`) | Lanza `IllegalTransactionStateException` y provoca rollback de la TX externa |
| `NESTED` | Crea una nueva TX | Lanza `NestedTransactionNotSupportedException` (`JpaDialect` no soporta savepoints) |

---

## 3. Logs Capturados por Caso

### 0. Flujo Real del Proyecto (`OrdenCompraService.cambiarEstado` con `REQUIRED`)

#### 0A. Llamada directa (`OrdenCompraController.cambiarEstado`)
```text
INFO  --- .i.TransaccionPropagacionIntegrationTest : --- 0A. Llamada directa: OrdenCompraController.cambiarEstado ---
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [com.distrimarket.compras.service.impl.OrdenCompraServiceImpl.cambiarEstado]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(1774039676<open>)] for JPA transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Exposing JPA transaction as JDBC [org.springframework.orm.jpa.vendor.HibernateJpaDialect$HibernateConnectionHandle@35ca8ec3]
TRACE --- o.s.t.i.TransactionInterceptor           : Getting transaction for [com.distrimarket.compras.service.impl.OrdenCompraServiceImpl.cambiarEstado]
INFO  --- c.d.c.s.impl.OrdenCompraServiceImpl      : [TX] OrdenCompraService.cambiarEstado | txActiva=true | txNombre=com.distrimarket.compras.service.impl.OrdenCompraServiceImpl.cambiarEstado
INFO  --- c.d.c.s.impl.OrdenCompraServiceImpl      : Cambiando estado de orden de compra ID: 10 a APROBADA
TRACE --- o.s.t.i.TransactionInterceptor           : Completing transaction for [com.distrimarket.compras.service.impl.OrdenCompraServiceImpl.cambiarEstado]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(1774039676<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(1774039676<open>)] after transaction
```

#### 0B. Llamada desde otro servicio (`FacturaCompraService.crear` → `OrdenCompraService.cambiarEstado`)
```text
INFO  --- .i.TransaccionPropagacionIntegrationTest : --- 0B. Llamada desde otro servicio: FacturaCompraService.crear -> OrdenCompraService.cambiarEstado ---
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [com.distrimarket.compras.service.impl.FacturaCompraServiceImpl.crear]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(203868223<open>)] for JPA transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Exposing JPA transaction as JDBC [org.springframework.orm.jpa.vendor.HibernateJpaDialect$HibernateConnectionHandle@66da2007]
TRACE --- o.s.t.i.TransactionInterceptor           : Getting transaction for [com.distrimarket.compras.service.impl.FacturaCompraServiceImpl.crear]
INFO  --- c.d.c.s.impl.FacturaCompraServiceImpl    : [TX] FacturaCompraService.crear | txActiva=true | txNombre=com.distrimarket.compras.service.impl.FacturaCompraServiceImpl.crear
INFO  --- c.d.c.s.impl.FacturaCompraServiceImpl    : Registrando factura de compra: Nro 001-001-0000123, Timbrado null, Proveedor ID 1
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Found thread-bound EntityManager [SessionImpl(203868223<open>)] for JPA transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Participating in existing transaction
TRACE --- o.s.t.i.TransactionInterceptor           : Getting transaction for [com.distrimarket.compras.service.impl.OrdenCompraServiceImpl.cambiarEstado]
INFO  --- c.d.c.s.impl.OrdenCompraServiceImpl      : [TX] OrdenCompraService.cambiarEstado | txActiva=true | txNombre=com.distrimarket.compras.service.impl.FacturaCompraServiceImpl.crear
INFO  --- c.d.c.s.impl.OrdenCompraServiceImpl      : Cambiando estado de orden de compra ID: 10 a FACTURADA
TRACE --- o.s.t.i.TransactionInterceptor           : Completing transaction for [com.distrimarket.compras.service.impl.OrdenCompraServiceImpl.cambiarEstado]
INFO  --- c.d.c.s.impl.FacturaCompraServiceImpl    : Factura de compra registrada con éxito. ID: 99, Total: 0
TRACE --- o.s.t.i.TransactionInterceptor           : Completing transaction for [com.distrimarket.compras.service.impl.FacturaCompraServiceImpl.crear]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(203868223<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(203868223<open>)] after transaction
```

---

### 1. `Propagation.REQUIRED`

#### 1A. Llamada directa
```text
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [...OrdenCompraPropagacionService.cambiarEstadoRequired]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(1630541163<open>)] for JPA transaction
TRACE --- o.s.t.i.TransactionInterceptor           : Getting transaction for [...OrdenCompraPropagacionService.cambiarEstadoRequired]
INFO  --- rationTest$OrdenCompraPropagacionService : [TARGET - REQUIRED] txActiva=true | txNombre=...OrdenCompraPropagacionService.cambiarEstadoRequired | ordenId=10 -> REQUIRED_DIRECTA
TRACE --- o.s.t.i.TransactionInterceptor           : Completing transaction for [...OrdenCompraPropagacionService.cambiarEstadoRequired]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(1630541163<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(1630541163<open>)] after transaction
```

#### 1B. Llamada desde otro servicio
```text
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [...FacturaCompraCallerService.invocarRequired]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(409229380<open>)] for JPA transaction
INFO  --- tegrationTest$FacturaCompraCallerService : [CALLER FacturaCompraService - REQUIRED] txActiva=true | txNombre=...FacturaCompraCallerService.invocarRequired
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Found thread-bound EntityManager [SessionImpl(409229380<open>)] for JPA transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Participating in existing transaction
INFO  --- rationTest$OrdenCompraPropagacionService : [TARGET - REQUIRED] txActiva=true | txNombre=...FacturaCompraCallerService.invocarRequired | ordenId=10 -> FACTURADA
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(409229380<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(409229380<open>)] after transaction
```

---

### 2. `Propagation.REQUIRES_NEW`

#### 2A. Llamada directa
```text
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [...OrdenCompraPropagacionService.cambiarEstadoRequiresNew]: PROPAGATION_REQUIRES_NEW,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(101923355<open>)] for JPA transaction
INFO  --- rationTest$OrdenCompraPropagacionService : [TARGET - REQUIRES_NEW] txActiva=true | txNombre=...OrdenCompraPropagacionService.cambiarEstadoRequiresNew | ordenId=10 -> REQUIRES_NEW_DIRECTA
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(101923355<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(101923355<open>)] after transaction
```

#### 2B. Llamada desde otro servicio
```text
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [...FacturaCompraCallerService.invocarRequiresNew]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(1283870672<open>)] for JPA transaction
INFO  --- tegrationTest$FacturaCompraCallerService : [CALLER FacturaCompraService - REQUIRES_NEW] txActiva=true | txNombre=...FacturaCompraCallerService.invocarRequiresNew
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Found thread-bound EntityManager [SessionImpl(1283870672<open>)] for JPA transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Suspending current transaction, creating new transaction with name [...OrdenCompraPropagacionService.cambiarEstadoRequiresNew]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(1657363827<open>)] for JPA transaction
INFO  --- rationTest$OrdenCompraPropagacionService : [TARGET - REQUIRES_NEW] txActiva=true | txNombre=...OrdenCompraPropagacionService.cambiarEstadoRequiresNew | ordenId=10 -> FACTURADA
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(1657363827<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(1657363827<open>)] after transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Resuming suspended transaction after completion of inner transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(1283870672<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(1283870672<open>)] after transaction
```

---

### 3. `Propagation.SUPPORTS`

#### 3A. Llamada directa
```text
TRACE --- o.s.t.i.TransactionInterceptor           : Getting transaction for [...OrdenCompraPropagacionService.cambiarEstadoSupports]
INFO  --- rationTest$OrdenCompraPropagacionService : [TARGET - SUPPORTS] txActiva=false | txNombre=...OrdenCompraPropagacionService.cambiarEstadoSupports | ordenId=10 -> SUPPORTS_DIRECTA
TRACE --- o.s.t.i.TransactionInterceptor           : Completing transaction for [...OrdenCompraPropagacionService.cambiarEstadoSupports]
```

#### 3B. Llamada desde otro servicio
```text
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [...FacturaCompraCallerService.invocarSupports]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(839959262<open>)] for JPA transaction
INFO  --- tegrationTest$FacturaCompraCallerService : [CALLER FacturaCompraService - SUPPORTS] txActiva=true | txNombre=...FacturaCompraCallerService.invocarSupports
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Found thread-bound EntityManager [SessionImpl(839959262<open>)] for JPA transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Participating in existing transaction
INFO  --- rationTest$OrdenCompraPropagacionService : [TARGET - SUPPORTS] txActiva=true | txNombre=...FacturaCompraCallerService.invocarSupports | ordenId=10 -> FACTURADA
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(839959262<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(839959262<open>)] after transaction
```

---

### 4. `Propagation.NOT_SUPPORTED`

#### 4A. Llamada directa
```text
TRACE --- o.s.t.i.TransactionInterceptor           : Getting transaction for [...OrdenCompraPropagacionService.cambiarEstadoNotSupported]
INFO  --- rationTest$OrdenCompraPropagacionService : [TARGET - NOT_SUPPORTED] txActiva=false | txNombre=...OrdenCompraPropagacionService.cambiarEstadoNotSupported | ordenId=10 -> NOT_SUPPORTED_DIRECTA
TRACE --- o.s.t.i.TransactionInterceptor           : Completing transaction for [...OrdenCompraPropagacionService.cambiarEstadoNotSupported]
```

#### 4B. Llamada desde otro servicio
```text
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [...FacturaCompraCallerService.invocarNotSupported]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(1298805460<open>)] for JPA transaction
INFO  --- tegrationTest$FacturaCompraCallerService : [CALLER FacturaCompraService - NOT_SUPPORTED] txActiva=true | txNombre=...FacturaCompraCallerService.invocarNotSupported
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Found thread-bound EntityManager [SessionImpl(1298805460<open>)] for JPA transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Suspending current transaction
INFO  --- rationTest$OrdenCompraPropagacionService : [TARGET - NOT_SUPPORTED] txActiva=false | txNombre=...OrdenCompraPropagacionService.cambiarEstadoNotSupported | ordenId=10 -> FACTURADA
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Resuming suspended transaction after completion of inner transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(1298805460<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(1298805460<open>)] after transaction
```

---

### 5. `Propagation.MANDATORY`

#### 5A. Llamada directa
Al no existir una transacción activa, `AbstractPlatformTransactionManager` lanza `IllegalTransactionStateException` antes de ingresar al método:
```text
INFO  --- .i.TransaccionPropagacionIntegrationTest : --- 5A. Llamada directa (MANDATORY - debe lanzar excepcion) ---
org.springframework.transaction.IllegalTransactionStateException: No existing transaction found for transaction marked with propagation 'mandatory'
```

#### 5B. Llamada desde otro servicio
```text
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [...FacturaCompraCallerService.invocarMandatory]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(1384116598<open>)] for JPA transaction
INFO  --- tegrationTest$FacturaCompraCallerService : [CALLER FacturaCompraService - MANDATORY] txActiva=true | txNombre=...FacturaCompraCallerService.invocarMandatory
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Found thread-bound EntityManager [SessionImpl(1384116598<open>)] for JPA transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Participating in existing transaction
INFO  --- rationTest$OrdenCompraPropagacionService : [TARGET - MANDATORY] txActiva=true | txNombre=...FacturaCompraCallerService.invocarMandatory | ordenId=10 -> FACTURADA
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(1384116598<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(1384116598<open>)] after transaction
```

---

### 6. `Propagation.NEVER`

#### 6A. Llamada directa
```text
TRACE --- o.s.t.i.TransactionInterceptor           : Getting transaction for [...OrdenCompraPropagacionService.cambiarEstadoNever]
INFO  --- rationTest$OrdenCompraPropagacionService : [TARGET - NEVER] txActiva=false | txNombre=...OrdenCompraPropagacionService.cambiarEstadoNever | ordenId=10 -> NEVER_DIRECTA
TRACE --- o.s.t.i.TransactionInterceptor           : Completing transaction for [...OrdenCompraPropagacionService.cambiarEstadoNever]
```

#### 6B. Llamada desde otro servicio
```text
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [...FacturaCompraCallerService.invocarNever]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(2033396031<open>)] for JPA transaction
INFO  --- tegrationTest$FacturaCompraCallerService : [CALLER FacturaCompraService - NEVER] txActiva=true | txNombre=...FacturaCompraCallerService.invocarNever
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Found thread-bound EntityManager [SessionImpl(2033396031<open>)] for JPA transaction
TRACE --- o.s.t.i.TransactionInterceptor           : Completing transaction for [...FacturaCompraCallerService.invocarNever] after exception: org.springframework.transaction.IllegalTransactionStateException: Existing transaction found for transaction marked with propagation 'never'
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction rollback
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Rolling back JPA transaction on EntityManager [SessionImpl(2033396031<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(2033396031<open>)] after transaction
```

---

### 7. `Propagation.NESTED`

#### 7A. Llamada directa
```text
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [...OrdenCompraPropagacionService.cambiarEstadoNested]: PROPAGATION_NESTED,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(667072570<open>)] for JPA transaction
INFO  --- rationTest$OrdenCompraPropagacionService : [TARGET - NESTED] txActiva=true | txNombre=...OrdenCompraPropagacionService.cambiarEstadoNested | ordenId=10 -> NESTED_DIRECTA
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction commit
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Committing JPA transaction on EntityManager [SessionImpl(667072570<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(667072570<open>)] after transaction
```

#### 7B. Llamada desde otro servicio
```text
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating new transaction with name [...FacturaCompraCallerService.invocarNested]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Opened new EntityManager [SessionImpl(552244031<open>)] for JPA transaction
INFO  --- tegrationTest$FacturaCompraCallerService : [CALLER FacturaCompraService - NESTED] txActiva=true | txNombre=...FacturaCompraCallerService.invocarNested
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Found thread-bound EntityManager [SessionImpl(552244031<open>)] for JPA transaction
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Creating nested transaction with name [...OrdenCompraPropagacionService.cambiarEstadoNested]
TRACE --- o.s.t.i.TransactionInterceptor           : Completing transaction for [...FacturaCompraCallerService.invocarNested] after exception: org.springframework.transaction.NestedTransactionNotSupportedException: JpaDialect does not support savepoints - check your JPA provider's capabilities
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Initiating transaction rollback
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Rolling back JPA transaction on EntityManager [SessionImpl(552244031<open>)]
DEBUG --- o.s.orm.jpa.JpaTransactionManager        : Closing JPA EntityManager [SessionImpl(552244031<open>)] after transaction
```
