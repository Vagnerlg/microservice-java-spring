# Tutorial — Testando a Plataforma Manualmente

Este guia permite explorar os três fluxos principais da plataforma sem executar o script `demo.sh` — útil para quem quer entender cada chamada individualmente ou adaptar os exemplos para sua própria conta.

---

## Para quem é este guia

| Perfil | Ponto de entrada |
|---|---|
| **Avaliador / entrevistador** | Você já tem Docker, curl e jq. Pule para [Pré-requisitos rápidos](#pré-requisitos-rápidos) e depois vá direto para os cenários. |
| **Developer externo** | Siga o [Setup completo](#setup-completo) para instalar dependências, clonar o repositório e subir a stack do zero. |

---

## Pré-requisitos rápidos (avaliador)

```bash
# Criar rede Docker (apenas na primeira vez)
docker network create platform-net

# Subir a stack
docker compose up -d

# Verificar que o Traefik está respondendo (aguarde ~60–90s)
curl -s http://localhost:8080/ -o /dev/null -w "HTTP %{http_code}\n"
# Esperado: HTTP 404 — Traefik respondendo, nenhuma rota padrão configurada
```

---

## Setup completo (developer externo)

### 1. Instalar dependências

**macOS (Homebrew):**
```bash
brew install curl jq
brew install --cask docker
```

**Ubuntu / Debian:**
```bash
sudo apt-get update && sudo apt-get install -y curl jq
# Docker: siga https://docs.docker.com/engine/install/ubuntu/
```

**Windows:** Use WSL2 ou Git Bash com Docker Desktop instalado.

### 2. Clonar o repositório

```bash
git clone https://github.com/Vagnerlg/microservice-java-spring.git
cd microservice-java-spring
```

### 3. Criar a rede Docker compartilhada

```bash
docker network create platform-net
```

### 4. Subir a stack completa

```bash
docker compose up -d
```

Isso sobe: Traefik, Keycloak, Kafka, Zookeeper, Schema Registry, MongoDB, PostgreSQL, Redis, Elasticsearch e todos os oito microserviços.

### 5. Verificar saúde da stack

Os serviços levam 60–90 segundos para inicializar. Verifique:

```bash
# Status de todos os containers
docker compose ps

# Testar que o Traefik está no ar
curl -s http://localhost:8080/ -o /dev/null -w "HTTP %{http_code}\n"
# Esperado: HTTP 404
```

---

## Definindo seu sufixo único

Os comandos abaixo usam `<sufixo>` para diferenciar seus produtos e usuários de outras execuções (evita conflito de nomes únicos). Substitua `<sufixo>` por qualquer string — recomendamos o timestamp Unix atual:

```bash
# macOS / Linux
date +%s
# Exemplo: 1720123456
```

Ao longo do tutorial, sempre que ver `<sufixo>`, use esse valor. Exemplos: `Notebook_1720123456`, `user_1720123456`.

---

## Cenário 1 — Happy Path: compra completa via Saga

**Fluxo:**
`cart.CHECKOUT` → order `PENDING` → `order.CREATED` → inventory verifica estoque → `stock-reservation.RESERVED` → order `CONFIRMED`

### Etapa 1.1 — Login admin e criação de 3 produtos

O usuário `admin` é pré-configurado no Keycloak na inicialização da stack. Usamos o token dele apenas para criar produtos no catálogo — não é o token do usuário comprador.

```bash
# Obter token do admin via Keycloak ROPC
ADMIN_TOKEN=$(curl -sf -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.data.accessToken')

echo "Token admin: ${ADMIN_TOKEN:0:60}..."
```

```bash
# Produto 1 — Notebook (categoria: eletronicos)
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Notebook_<sufixo>",
    "description": "Notebook para desenvolvedores, 16GB RAM",
    "price": 4999.99,
    "category": "eletronicos"
  }' | jq '.data'
```

Anote o campo `id` retornado — você vai usá-lo como `<product1-id>` nos próximos passos.

```bash
# Produto 2 — Mouse (categoria: perifericos)
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Mouse_<sufixo>",
    "description": "Mouse sem fio ergonomico",
    "price": 199.90,
    "category": "perifericos"
  }' | jq '.data'
```

```bash
# Produto 3 — Teclado (categoria: perifericos)
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Teclado_<sufixo>",
    "description": "Teclado mecanico compacto",
    "price": 349.90,
    "category": "perifericos"
  }' | jq '.data'
```

O `inventory-service` consome o evento `product.CREATED` via Kafka e inicializa automaticamente **10 unidades** de estoque por produto — sem nenhuma chamada HTTP adicional.

### Etapa 1.2 — Aguardar indexação no Elasticsearch

O `search-service` consome `product.CREATED` do Kafka e indexa os produtos no Elasticsearch. Essa etapa é assíncrona e pode levar até 15 segundos.

Execute o comando abaixo repetidamente até `totalElements` aparecer como `3`:

```bash
curl -s "http://localhost:8080/api/search?q=_<sufixo>" \
  | jq '{totalElements, produtos: [.content[] | {name}]}'
```

### Etapa 1.3 — Registrar usuário comprador

O `auth-service` cria o usuário no Keycloak via Admin API e publica `user.CREATED` no tópico Kafka `user`. O `user-service` consome esse evento e cria o perfil no PostgreSQL de forma assíncrona.

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user_<sufixo>",
    "name": "Avaliador Demo",
    "password": "senha123"
  }' | jq '.data'
```

### Etapa 1.4 — Login e obtenção do JWT

```bash
ACCESS_TOKEN=$(curl -sf -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user_<sufixo>",
    "password": "senha123"
  }' \
  | jq -r '.data.accessToken')

echo "JWT: ${ACCESS_TOKEN:0:60}..."
```

Inspecione os claims principais do token (opcional):

```bash
echo $ACCESS_TOKEN \
  | jq -R 'split(".") | .[1] | @base64d | fromjson | {sub, preferred_username, exp}'
```

### Etapa 1.5 — Verificar perfil criado pelo Kafka

O `user-service` criou o perfil automaticamente ao consumir `user.CREATED`. Nenhuma chamada HTTP foi feita diretamente ao `user-service` no momento do cadastro.

```bash
curl -s http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq '.data'
```

### Etapa 1.6 — Adicionar produto ao carrinho

O `cart-service` armazena o carrinho no Redis com a chave `cart:{userId}` e TTL de 7 dias.

```bash
curl -s -X POST http://localhost:8080/api/carts/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "productId": "<product1-id>",
    "name": "Notebook_<sufixo>",
    "price": 4999.99,
    "quantity": 1
  }' | jq '.data'
```

### Etapa 1.7 — Checkout: início da Saga

O `cart-service` publica `cart.CHECKOUT` no tópico Kafka `cart` e apaga o carrinho atomicamente. A partir daqui, a Saga é coreografada via Kafka — não há orquestrador central.

```bash
curl -s -X POST http://localhost:8080/api/carts/checkout \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -w "\nHTTP %{http_code}\n"
# Esperado: HTTP 204
```

### Etapa 1.8 — Acompanhar a Saga até CONFIRMED

Fluxo assíncrono em andamento:

```
cart.CHECKOUT
  → order-service cria pedido (PENDING)
  → publica order.CREATED
    → inventory-service verifica estoque (10 unidades disponíveis)
    → publica stock-reservation.RESERVED
      → order-service atualiza pedido para CONFIRMED
```

Execute a cada 2–3 segundos até o status ser `CONFIRMED` (máximo ~15s):

```bash
curl -s http://localhost:8080/api/orders \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq '.data.content[0] | {id, status}'
```

Quando `CONFIRMED`, consulte o pedido completo:

```bash
ORDER_ID=$(curl -sf http://localhost:8080/api/orders \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq -r '.data.content[0].id')

curl -s "http://localhost:8080/api/orders/$ORDER_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq '.data | {id, status, totalPrice, items: [.items[] | {name, quantity, price}]}'
```

---

## Cenário 2 — Estoque esgotado: Saga cancela o pedido

**Fluxo:**
`cart.CHECKOUT` → order `PENDING` → `order.CREATED` → inventory recusa (qty > 10) → `stock-reservation.UNAVAILABLE` → order `CANCELLED`

> Repita as etapas 1.1 a 1.5 para criar produtos e um usuário logado. Use um `<sufixo>` diferente se quiser isolar os dados de execuções anteriores.

### Etapa 2.1 — Adicionar 11 unidades ao carrinho

O estoque inicial é de 10 unidades por produto. Solicitar 11 força o `inventory-service` a recusar a reserva.

```bash
curl -s -X POST http://localhost:8080/api/carts/items \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "productId": "<product1-id>",
    "name": "Notebook_<sufixo>",
    "price": 4999.99,
    "quantity": 11
  }' | jq '.data'
```

### Etapa 2.2 — Checkout: Saga condenada ao cancelamento

```bash
curl -s -X POST http://localhost:8080/api/carts/checkout \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -w "\nHTTP %{http_code}\n"
# Esperado: HTTP 204 — o pedido é criado como PENDING; o cancelamento vem via Kafka
```

### Etapa 2.3 — Acompanhar a Saga até CANCELLED

Execute a cada 2–3 segundos até o status ser `CANCELLED` (máximo ~15s):

```bash
curl -s http://localhost:8080/api/orders \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq '.data.content[0] | {id, status}'
```

Quando cancelado, verifique o motivo:

```bash
ORDER_ID=$(curl -sf http://localhost:8080/api/orders \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq -r '.data.content[0].id')

curl -s "http://localhost:8080/api/orders/$ORDER_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq '.data | {id, status, cancellationReason}'
```

---

## Cenário 3 — Busca full-text CQRS com Elasticsearch

**Arquitetura:**
`product-service` (MongoDB) → evento `product.CREATED` → `search-service` indexa no Elasticsearch → `GET /api/search`

O `search-service` é um **read model puro** — sem banco transacional próprio. Todo o estado chega via Kafka.

> Requer os 3 produtos criados na Etapa 1.1. Se já executou o Cenário 1, reutilize o mesmo `<sufixo>`.

### Etapa 3.1 — Busca exata por nome

```bash
curl -s "http://localhost:8080/api/search?q=Notebook_<sufixo>" \
  | jq '{totalElements, results: [.content[] | {name, category, price}]}'
# Esperado: totalElements: 1
```

### Etapa 3.2 — Busca parcial: todos os produtos do sufixo

```bash
curl -s "http://localhost:8080/api/search?q=_<sufixo>" \
  | jq '{totalElements, results: [.content[] | {name, category, price}]}'
# Esperado: totalElements: 3 (Notebook, Mouse, Teclado)
```

### Etapa 3.3 — Busca com filtro de categoria

```bash
curl -s "http://localhost:8080/api/search?q=_<sufixo>&category=perifericos" \
  | jq '{totalElements, results: [.content[] | {name, category, price}]}'
# Esperado: totalElements: 2 (Mouse e Teclado)
```

---

## Explore a plataforma

Após rodar os cenários, navegue nas UIs de observabilidade e infraestrutura:

| Ferramenta | URL | O que observar |
|---|---|---|
| **Grafana** | `http://localhost:8080/grafana` | Traces (Tempo), logs com `traceId` (Loki), métricas JVM (Prometheus) |
| **Prometheus** | `http://localhost:8080/prometheus` | Métricas de JVM e consumer lag dos Kafka consumers |
| **Kafka UI** | `http://localhost:8080/kafka` | Tópicos `cart`, `order`, `stock-reservation`; mensagens e consumer groups |
| **Keycloak Admin** | `http://localhost:8080/keycloak` | Realm `ecommerce`, usuários criados, roles e sessões |
| **Mongo Express** | `http://localhost:8080/mongo` | Coleção `products` no MongoDB com os produtos criados |
| **Traefik Dashboard** | `http://localhost:8090/dashboard/` | Roteamento HTTP, middlewares de strip de prefixo, status das rotas |

**Credenciais:**

| Ferramenta | Usuário | Senha |
|---|---|---|
| Grafana | `admin` | `admin` |
| Keycloak Admin Console | `admin` | `admin` |
| API da plataforma (admin) | `admin` | `admin123` |
