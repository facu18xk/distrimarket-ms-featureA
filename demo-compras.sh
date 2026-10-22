#!/usr/bin/env bash
# ────────────────────────────────────────────────────────────────────────
#  demo-compras.sh · Demo en vivo del microservicio de Compras (Distrimarket)
# ────────────────────────────────────────────────────────────────────────
#  Antes de grabar:
#    export DEP=__ PROD=__ MEDIO=__     (IDs preparados, ver checklist del guion)
#    Stack corriendo (python3 scripts/run_stack.py en distrimarket-common)
#    ⚠️  En CADA toma usá RUC y NUM_FACTURA nuevos; si no, el POST da 409.
#
#  Uso:
#    ./demo-compras.sh          → pausa con Enter entre pasos (para grabar)
#    ./demo-compras.sh --yes    → sin pausas (ensayo completo)
# ────────────────────────────────────────────────────────────────────────
set -u

API="${API:-http://localhost:8083}"
GW="${GW:-http://localhost:8080}"
TMP="${TMPDIR:-/tmp}"

DEP="${DEP:?Falta export DEP}"
PROD="${PROD:?Falta export PROD}"
MEDIO="${MEDIO:?Falta export MEDIO}"
NUM_FACTURA="${NUM_FACTURA:-001-001-0000123}"
TIMBRADO="${TIMBRADO:-15894231}"
RUC="${RUC:-80012345-6}"
FECHA="${FECHA:-$(date +%F)}"

NOPAUSE=0; [ "${1:-}" = "--yes" ] && NOPAUSE=1
PROV=""; FACT=""

pause() { [ "$NOPAUSE" = "1" ] || { read -rp "⏎ Enter para continuar..." _; echo; }; }
title() { printf '\n━━━ %s\n' "$1"; }
show()  { printf '\n$ %s\n\n' "$1"; }
ask()   { curl -sS -o "$TMP/demo-compras.out" -w 'HTTP %{http_code}\n' "$@" \
            && { python3 -m json.tool "$TMP/demo-compras.out" 2>/dev/null || cat "$TMP/demo-compras.out"; echo; }; }
json()  { printf '%s' "$2" > "$TMP/demo-compras.$1.json"; echo "$TMP/demo-compras.$1.json"; }

# ── A1 · Crear proveedor → 201 ──────────────────────────────────────────
title "A1 · Crear proveedor (esperado: 201)"
A1=$(json a1 "$(printf '{"nombreFantasia":"Distribuidora del Este","persona":{"tipoPersona":"JURIDICA","nombreCompleto":"Distribuidora del Este S.A.","ruc":"%s"}}' "$RUC")")
show "curl -X POST $API/compras/proveedores -H 'Content-Type: application/json' -d @$A1"
ask -X POST "$API/compras/proveedores" -H "Content-Type: application/json" --data @"$A1"
PROV=$(python3 -c "import json;print(json.load(open('$TMP/demo-compras.out'))['idProveedor'])" 2>/dev/null)
[ -n "$PROV" ] || { echo "❌ No se creó el proveedor (¿stack arriba? ¿RUC repetido?)"; exit 1; }
echo "→ PROV=$PROV"; pause

# ── A2 · Acuerdo M2M proveedor-producto → 201 ───────────────────────────
title "A2 · Acuerdo proveedor-producto, relación M2M (esperado: 201)"
A2=$(json a2 "$(printf '{"idProveedor":%s,"idProducto":%s,"precioCostoAcordado":4500.00,"codigoProveedor":"PROV-SKU-001"}' "$PROV" "$PROD")")
show "curl -X POST $API/compras/proveedores-productos -H 'Content-Type: application/json' -d @$A2"
ask -X POST "$API/compras/proveedores-productos" -H "Content-Type: application/json" --data @"$A2"
pause

# ── A3 · Stock ANTES (Inventario, vía gateway) ──────────────────────────
title "A3 · Stock ANTES (Inventario, vía gateway)"
show "curl -sS $GW/inventario/stock-depositos/deposito/$DEP/producto/$PROD"
curl -sS "$GW/inventario/stock-depositos/deposito/$DEP/producto/$PROD" | python3 -m json.tool
pause

# ── A4 · Registrar factura de compra → 201 ──────────────────────────────
title "A4 · Registrar factura de compra (esperado: 201)"
A4=$(json a4 "$(printf '{"numeroFactura":"%s","fechaEmision":"%s","idProveedor":%s,"idDeposito":%s,"idMedioPago":%s,"timbrado":"%s","condicionCompra":"CONTADO","detalles":[{"idProducto":%s,"cantidad":10,"precioUnitario":4500,"porcentajeIva":10}]}' "$NUM_FACTURA" "$FECHA" "$PROV" "$DEP" "$MEDIO" "$TIMBRADO" "$PROD")")
show "curl -X POST $API/compras/facturas -H 'Content-Type: application/json' -d @$A4"
ask -X POST "$API/compras/facturas" -H "Content-Type: application/json" --data @"$A4"
FACT=$(python3 -c "import json;print(json.load(open('$TMP/demo-compras.out'))['idFacturaCompra'])" 2>/dev/null)
[ -n "$FACT" ] || { echo "❌ No se creó la factura (¿NUM_FACTURA o TIMBRADO repetido?)"; exit 1; }
echo "→ FACT=$FACT"; pause

# ── A5 · Stock DESPUÉS (debe haber subido 10) ───────────────────────────
title "A5 · Stock DESPUÉS (debe ser el anterior + 10)"
show "curl -sS $GW/inventario/stock-depositos/deposito/$DEP/producto/$PROD"
curl -sS "$GW/inventario/stock-depositos/deposito/$DEP/producto/$PROD" | python3 -m json.tool
pause

# ── B1 · Consultar por ID → 200 ─────────────────────────────────────────
title "B1 · Consultar factura por ID (esperado: 200)"
show "curl -sS $API/compras/facturas/$FACT"
curl -sS "$API/compras/facturas/$FACT" | python3 -m json.tool
pause

# ── B2 · Listado paginado → 200 ─────────────────────────────────────────
title "B2 · Listado paginado (esperado: 200)"
show "curl -sS \"$API/compras/facturas?page=0&size=3&sort=id,desc\""
curl -sS "$API/compras/facturas?page=0&size=3&sort=id,desc" | python3 -m json.tool
pause

# ── B3 · Búsqueda con filtros + paginación → 200 (endpoint de filtros) ───
title "B3 · Búsqueda con filtros + paginación (esperado: 200)"
show "curl -sS \"$API/compras/facturas/buscar?proveedorId=$PROV&estado=ACTIVA&page=0&size=5&sort=fechaEmision,desc\""
curl -sS "$API/compras/facturas/buscar?proveedorId=$PROV&estado=ACTIVA&page=0&size=5&sort=fechaEmision,desc" | python3 -m json.tool
pause

# ── C1 · Actualizar con PUT → 200 ───────────────────────────────────────
title "C1 · Actualizar proveedor con PUT (esperado: 200)"
C1=$(json c1 '{"nombreFantasia":"Distribuidora del Este (actualizado)","persona":{"tipoPersona":"JURIDICA","nombreCompleto":"Distribuidora del Este S.A.","telefono":"0981-123456"}}')
show "curl -X PUT $API/compras/proveedores/$PROV -H 'Content-Type: application/json' -d @$C1"
ask -X PUT "$API/compras/proveedores/$PROV" -H "Content-Type: application/json" --data @"$C1"
pause

# ── C2 · Borrado lógico → 204 y activo=false ────────────────────────────
title "C2 · Borrado lógico (esperado: 204) y verificación"
show "curl -X DELETE $API/compras/facturas/$FACT"
ask -X DELETE "$API/compras/facturas/$FACT"
show "curl -sS $API/compras/facturas/$FACT   # debe mostrar \"activo\": false"
curl -sS "$API/compras/facturas/$FACT" | python3 -m json.tool
pause

# ── D1 · Duplicado → 409 ────────────────────────────────────────────────
title "D1 · Factura duplicada (esperado: 409)"
show "curl -X POST $API/compras/facturas -H 'Content-Type: application/json' -d @$A4   # mismo body"
ask -X POST "$API/compras/facturas" -H "Content-Type: application/json" --data @"$A4"
pause

# ── D2 · Body inválido → 400 ────────────────────────────────────────────
title "D2 · Body inválido (esperado: 400)"
show "curl -X POST $API/compras/proveedores -H 'Content-Type: application/json' -d '{}'"
ask -X POST "$API/compras/proveedores" -H "Content-Type: application/json" -d '{}'
pause

# ── D3 · ID inexistente → 404 ───────────────────────────────────────────
title "D3 · ID inexistente (esperado: 404)"
show "curl -sS $API/compras/facturas/99999999"
ask "$API/compras/facturas/99999999"

printf '\n✅ Demo completa\n'
