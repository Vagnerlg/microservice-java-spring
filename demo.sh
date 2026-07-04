#!/usr/bin/env bash
# Windows: execute via WSL2 ou Git Bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ─── Config ──────────────────────────────────────────────────────────────────
BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin123}"
AUTO=false

for arg in "$@"; do
  case $arg in --auto) AUTO=true ;; esac
done

# ─── Colors ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; BLUE='\033[0;34m'
YELLOW='\033[1;33m'; BOLD='\033[1m'; NC='\033[0m'

# ─── Helpers ─────────────────────────────────────────────────────────────────
step() {
  echo -e "\n${BLUE}►${NC} ${BOLD}$1${NC}"
  if [ "$AUTO" = "false" ]; then
    printf "  ${YELLOW}[Enter para continuar]${NC} "; read -r
  else
    sleep 1
  fi
}
info()    { echo -e "  ${BLUE}$1${NC}"; }
success() { echo -e "  ${GREEN}✓ $1${NC}"; }
warn()    { echo -e "  ${YELLOW}⚠ $1${NC}"; }
header()  {
  echo -e "\n${BOLD}═══════════════════════════════════════════════════${NC}"
  echo -e "${BOLD}  $1${NC}"
  echo -e "${BOLD}═══════════════════════════════════════════════════${NC}"
}

# ─── Error trap ──────────────────────────────────────────────────────────────
trap 'echo -e "\n${RED}✗ Erro inesperado. Verifique se todos os containers estão healthy:${NC}\n  docker compose ps" >&2' ERR

# ─── Dependency check + Docker fallback ──────────────────────────────────────
check_deps() {
  local missing=()
  command -v curl &>/dev/null || missing+=("curl")
  command -v jq   &>/dev/null || missing+=("jq")
  [ ${#missing[@]} -eq 0 ] && return 0

  warn "Dependências ausentes: ${missing[*]}"

  if ! command -v docker &>/dev/null; then
    echo -e "${RED}Instale curl e jq e tente novamente.${NC}"; exit 1
  fi

  info "Re-executando via Docker (platform-net → traefik:8080)..."
  exec docker run --rm -it \
    --network platform-net \
    -e BASE_URL=http://traefik:8080 \
    -e ADMIN_USER="$ADMIN_USER" \
    -e ADMIN_PASS="$ADMIN_PASS" \
    -v "${SCRIPT_DIR}:/workspace" \
    alpine sh -c "apk add -q curl jq && /workspace/demo.sh $*"
}

# ─── Stack health check ──────────────────────────────────────────────────────
check_stack() {
  info "Verificando stack em ${BASE_URL}..."
  if ! curl -s --max-time 5 -o /dev/null "${BASE_URL}/"; then
    echo -e "${RED}✗ Stack não está acessível.${NC}"
    echo -e "  Execute: ${YELLOW}docker compose up -d${NC}"
    echo -e "  Aguarde os serviços iniciarem e tente novamente."
    exit 1
  fi
  success "Stack acessível"
}

# ─── Shared state ────────────────────────────────────────────────────────────
TS=$(date +%s)
PRODUCT1_ID=""; PRODUCT2_ID=""; PRODUCT3_ID=""
ACCESS_TOKEN=""

# ─── Shared setup: 3 products ────────────────────────────────────────────────
setup_products() {
  step "Login admin → obtendo token para criar produtos (Keycloak ROPC)"
  local ADMIN_TOKEN
  ADMIN_TOKEN=$(curl -sf -X POST "${BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}" \
    | jq -r '.data.accessToken')
  success "Token admin obtido"

  step "Criando 3 produtos no catálogo (product-service → MongoDB)"
  PRODUCT1_ID=$(curl -sf -X POST "${BASE_URL}/api/products" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -d "{\"name\":\"Notebook_${TS}\",\"description\":\"Notebook para desenvolvedores, 16GB RAM\",\"price\":4999.99,\"category\":\"eletronicos\"}" \
    | jq -r '.data.id')

  PRODUCT2_ID=$(curl -sf -X POST "${BASE_URL}/api/products" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -d "{\"name\":\"Mouse_${TS}\",\"description\":\"Mouse sem fio ergonomico\",\"price\":199.90,\"category\":\"perifericos\"}" \
    | jq -r '.data.id')

  PRODUCT3_ID=$(curl -sf -X POST "${BASE_URL}/api/products" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -d "{\"name\":\"Teclado_${TS}\",\"description\":\"Teclado mecanico compacto\",\"price\":349.90,\"category\":\"perifericos\"}" \
    | jq -r '.data.id')

  success "3 produtos criados  [eletronicos: 1, perifericos: 2]"

  step "Aguardando search-service indexar via Kafka → Elasticsearch (max 15s)"
  info "product-service publica product.CREATED → search-service consome e indexa no ES"
  local attempts=0
  until curl -sf "${BASE_URL}/api/search?q=Notebook_${TS}" \
      | jq -e '.totalElements > 0' > /dev/null 2>&1; do
    attempts=$((attempts + 1))
    [ "$attempts" -ge 15 ] && { echo -e "\n${RED}✗ Timeout: search-service não indexou em 15s.${NC}"; exit 1; }
    printf "."
    sleep 1
  done
  echo ""
  success "Indexação concluída em ${attempts}s"
}

# ─── Shared user: register + login ───────────────────────────────────────────
register_and_login() {
  step "Registrando usuário (auth-service → Keycloak Admin API + publica user.CREATED no Kafka)"
  curl -sf -X POST "${BASE_URL}/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"user_${TS}\",\"name\":\"Avaliador Demo\",\"password\":\"senha123\"}" \
    | jq '.data'

  step "Login → JWT emitido pelo Keycloak"
  ACCESS_TOKEN=$(curl -sf -X POST "${BASE_URL}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"user_${TS}\",\"password\":\"senha123\"}" \
    | jq -r '.data.accessToken')
  success "JWT obtido"
  info "Claims principais:"
  jq -R 'split(".") | .[1] | @base64d | fromjson | {sub, preferred_username, exp}' \
    <<< "$ACCESS_TOKEN" 2>/dev/null || true
}

# ─── Poll: espera order atingir status esperado ───────────────────────────────
poll_order_status() {
  local EXPECTED_STATUS="$1"
  local ORDER_ID=""
  local attempts=0

  until ORDER_ID=$(curl -sf "${BASE_URL}/api/orders" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      | jq -r '.data.content[0].id // empty') && [ -n "$ORDER_ID" ]; do
    attempts=$((attempts + 1))
    [ "$attempts" -ge 15 ] && { echo -e "\n${RED}✗ Timeout: pedido não criado em 15s.${NC}"; exit 1; }
    sleep 1
  done

  attempts=0
  until curl -sf "${BASE_URL}/api/orders/${ORDER_ID}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      | jq -e ".data.status == \"${EXPECTED_STATUS}\"" > /dev/null 2>&1; do
    attempts=$((attempts + 1))
    [ "$attempts" -ge 15 ] && {
      echo -e "\n${RED}✗ Timeout: pedido não atingiu ${EXPECTED_STATUS} em 15s.${NC}"; exit 1
    }
    printf "."
    sleep 1
  done
  echo ""
  echo "$ORDER_ID"
}

# ─── Explore ─────────────────────────────────────────────────────────────────
show_explore() {
  header "EXPLORE A PLATAFORMA"
  echo ""
  echo -e "  ${BOLD}Observabilidade${NC}"
  echo -e "  ${BLUE}Grafana${NC}         → http://localhost:8080/grafana"
  echo -e "  ${BLUE}Prometheus${NC}      → http://localhost:8080/prometheus"
  echo ""
  echo -e "  ${BOLD}Infraestrutura${NC}"
  echo -e "  ${BLUE}Kafka UI${NC}        → http://localhost:8080/kafka"
  echo -e "  ${BLUE}Keycloak Admin${NC}  → http://localhost:8080/keycloak"
  echo -e "  ${BLUE}Mongo Express${NC}   → http://localhost:8080/mongo"
  echo -e "  ${BLUE}Traefik${NC}         → http://localhost:8090/dashboard/"
  echo ""
  echo -e "  ${BOLD}Busca live${NC}"
  echo -e "  ${YELLOW}curl '${BASE_URL}/api/search?q=_${TS}'${NC}"
  echo ""
}

# ─── Cenário 1: Happy path ────────────────────────────────────────────────────
scenario_happy_path() {
  header "CENÁRIO 1 — Happy Path: compra completa via Saga"

  setup_products
  register_and_login

  step "GET /users/me → user-service consumiu user.CREATED via Kafka e criou o perfil (PostgreSQL)"
  curl -sf "${BASE_URL}/api/users/me" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" | jq '.data'

  step "Adicionando Notebook ao carrinho (cart-service → Redis, TTL 7 dias)"
  curl -sf -X POST "${BASE_URL}/api/carts/items" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -d "{\"productId\":\"${PRODUCT1_ID}\",\"name\":\"Notebook_${TS}\",\"price\":4999.99,\"quantity\":1}" \
    | jq '.data'

  step "Checkout → cart-service publica cart.CHECKOUT no Kafka"
  info "Saga: cart.CHECKOUT → order PENDING → order.CREATED → inventory reserva → stock.RESERVED → order CONFIRMED"
  curl -sf -X POST "${BASE_URL}/api/carts/checkout" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" -o /dev/null -w "HTTP %{http_code}\n"
  success "Checkout realizado"

  step "Aguardando Saga: order PENDING → CONFIRMED (max 15s)"
  local ORDER_ID
  ORDER_ID=$(poll_order_status "CONFIRMED")

  step "Pedido CONFIRMED — detalhes finais"
  curl -sf "${BASE_URL}/api/orders/${ORDER_ID}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    | jq '.data | {id, status, totalPrice, items: [.items[] | {name, quantity, price}]}'

  echo ""
  success "Compra concluída. Pedido CONFIRMED via Saga coreografada."

  show_explore
}

# ─── Cenário 2: Estoque esgotado ──────────────────────────────────────────────
scenario_stock_exhausted() {
  header "CENÁRIO 2 — Estoque esgotado: Saga cancela pedido via Kafka"

  setup_products
  register_and_login

  step "Adicionando 11 unidades ao carrinho (estoque inicial = 10 por produto)"
  warn "inventory-service vai detectar quantity=11 > availableQuantity=10 e recusar a reserva"
  curl -sf -X POST "${BASE_URL}/api/carts/items" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -d "{\"productId\":\"${PRODUCT1_ID}\",\"name\":\"Notebook_${TS}\",\"price\":4999.99,\"quantity\":11}" \
    | jq '.data'

  step "Checkout → Saga condenada ao cancelamento"
  info "Fluxo: cart.CHECKOUT → order PENDING → order.CREATED → inventory recusa → stock.UNAVAILABLE → order CANCELLED"
  curl -sf -X POST "${BASE_URL}/api/carts/checkout" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" -o /dev/null -w "HTTP %{http_code}\n"
  success "Checkout realizado — pedido criado como PENDING"

  step "Aguardando Saga: order PENDING → CANCELLED (max 15s)"
  local ORDER_ID
  ORDER_ID=$(poll_order_status "CANCELLED")

  step "Pedido CANCELLED — motivo do cancelamento"
  curl -sf "${BASE_URL}/api/orders/${ORDER_ID}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    | jq '.data | {id, status, cancellationReason}'

  echo ""
  success "Saga funcionando: pedido cancelado automaticamente por falta de estoque."
  info "Tópicos Kafka envolvidos: cart → order → stock-reservation"

  show_explore
}

# ─── Cenário 3: Busca full-text ───────────────────────────────────────────────
scenario_search() {
  header "CENÁRIO 3 — Busca full-text: CQRS com Elasticsearch"

  setup_products

  step "Busca exata: q=Notebook_${TS}"
  info "Read model no Elasticsearch atualizado via Kafka (product.CREATED → search-service)"
  curl -sf "${BASE_URL}/api/search?q=Notebook_${TS}" \
    | jq '{totalElements, results: [.content[] | {name, category, price}]}'

  step "Busca parcial: q=_${TS}  →  todos os 3 produtos do demo"
  curl -sf "${BASE_URL}/api/search?q=_${TS}" \
    | jq '{totalElements, results: [.content[] | {name, category, price}]}'

  step "Busca com filtro: q=_${TS}&category=perifericos  →  Mouse + Teclado"
  info "Filtra apenas a categoria 'perifericos' (2 de 3 produtos)"
  curl -sf "${BASE_URL}/api/search?q=_${TS}&category=perifericos" \
    | jq '{totalElements, results: [.content[] | {name, category, price}]}'

  echo ""
  success "CQRS funcionando: read model no Elasticsearch atualizado via eventos Kafka."
  info "product-service (MongoDB) → product.CREATED → search-service (Elasticsearch)"

  show_explore
}

# ─── Menu principal ───────────────────────────────────────────────────────────
main() {
  echo -e "\n${BOLD}╔═══════════════════════════════════════════════════╗${NC}"
  echo -e "${BOLD}║    microservice-java-spring — Demo interativo     ║${NC}"
  echo -e "${BOLD}╚═══════════════════════════════════════════════════╝${NC}"
  echo ""
  echo -e "  ${GREEN}1)${NC} Happy path       — compra completa (Saga CONFIRMED)"
  echo -e "  ${GREEN}2)${NC} Estoque esgotado — Saga cancela pedido automaticamente"
  echo -e "  ${GREEN}3)${NC} Busca full-text  — CQRS com Elasticsearch"
  echo -e "  ${GREEN}q)${NC} Sair"
  echo ""
  printf "  Escolha: "; read -r choice; echo ""

  case "$choice" in
    1) scenario_happy_path ;;
    2) scenario_stock_exhausted ;;
    3) scenario_search ;;
    q|Q) echo "Até logo!"; exit 0 ;;
    *) echo -e "${RED}Opção inválida.${NC}"; main ;;
  esac
}

# ─── Entry point ─────────────────────────────────────────────────────────────
check_deps "$@"
check_stack
main
