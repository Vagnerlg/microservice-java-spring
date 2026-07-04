#!/usr/bin/env bash
# Windows: execute via WSL2 ou Git Bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ─── Config ──────────────────────────────────────────────────────────────────
FORCE=false
for arg in "$@"; do
  case $arg in --force) FORCE=true ;; esac
done

# ─── Colors ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BOLD='\033[1m'; NC='\033[0m'

success() { echo -e "  ${GREEN}✓ $1${NC}"; }
skip()    { echo -e "  ${YELLOW}– $1${NC}"; }

# ─── Apresentação ─────────────────────────────────────────────────────────────
echo -e "\n${BOLD}╔═══════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║    microservice-java-spring — Limpeza completa    ║${NC}"
echo -e "${BOLD}╚═══════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  Isso vai remover:"
echo -e "  ${YELLOW}•${NC} Todos os containers da stack"
echo -e "  ${YELLOW}•${NC} Todos os volumes (MongoDB, PostgreSQL, Redis, Elasticsearch)"
echo -e "  ${YELLOW}•${NC} Todas as imagens Docker usadas pelo projeto"
echo -e "  ${YELLOW}•${NC} A rede ${BOLD}platform-net${NC}"
echo ""
echo -e "  ${RED}⚠  Esta operação é irreversível.${NC}"
echo -e "  ${YELLOW}⚠  O próximo 'docker compose up' vai baixar ~2 GB de imagens.${NC}"
echo ""

# ─── Confirmação ─────────────────────────────────────────────────────────────
if [ "$FORCE" = "false" ]; then
  printf "  Confirma a limpeza? [s/N]: "; read -r answer
  if [[ ! "$answer" =~ ^[sS]$ ]]; then
    echo -e "  Cancelado."; exit 0
  fi
fi

echo ""
cd "$SCRIPT_DIR"

# ─── Containers + volumes + imagens ──────────────────────────────────────────
echo -e "${BOLD}Removendo containers, volumes e imagens...${NC}"
if docker compose down --rmi all -v 2>/dev/null; then
  success "Containers, volumes e imagens removidos"
else
  skip "Nenhum container em execução ou imagens já ausentes"
fi

# ─── Rede ─────────────────────────────────────────────────────────────────────
echo -e "${BOLD}Removendo rede platform-net...${NC}"
if docker network rm platform-net 2>/dev/null; then
  success "Rede platform-net removida"
else
  skip "Rede platform-net não encontrada"
fi

# ─── Conclusão ────────────────────────────────────────────────────────────────
echo ""
success "Limpeza concluída. Sua máquina está como antes do git clone."
echo ""
echo -e "  Para recriar a stack:"
echo -e "  ${YELLOW}docker network create platform-net${NC}"
echo -e "  ${YELLOW}docker compose up -d${NC}"
echo ""
