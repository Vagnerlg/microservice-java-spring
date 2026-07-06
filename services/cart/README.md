# cart-service

[![cart-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/cart-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/cart-quality.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)
![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-producer-231F20?logo=apachekafka)

Microserviço de carrinho de compras da plataforma de e-commerce. Armazena o carrinho de cada usuário no **Redis** (TTL de 7 dias) e, ao fazer checkout, publica o evento `cart.CHECKOUT` no **Kafka** para ser consumido pelo `order-service`.

Todas as rotas exigem JWT emitido pelo Keycloak — o `userId` é lido do claim `sub` do token, sem precisar passar o ID na URL.

---

## Índice

- [Stack](#stack)
- [Arquitetura interna (DDD)](#arquitetura-interna-ddd)
- [Autenticação](#autenticação)
- [API Reference](#api-reference)
- [Eventos Kafka](#eventos-kafka)
- [Como rodar localmente](#como-rodar-localmente)
- [Testes e qualidade](#testes-e-qualidade)
- [CI](#ci)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (records) |
| Framework | Spring Boot 4.0 |
| Storage | Redis 7 (TTL 7 dias por carrinho) |
| Mensageria | Apache Kafka (producer) |
| Segurança | Spring Security OAuth2 Resource Server (JWT) |
| Observabilidade | OpenTelemetry + Grafana (Tempo · Loki · Prometheus) |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Qualidade | JaCoCo · SpotBugs · PMD |
| CI | GitHub Actions |

---

## Arquitetura interna (DDD)

```
com.github.vagnerlg.cart/
├── domain/               # Cart (record), CartItem (record), CartRepository (porta),
│                         # CartEventPublisher (porta), exceções de domínio
├── application/          # CartService — casos de uso (getCart, addItem, updateItem,
│                         # removeItem, clearCart, checkout)
└── infrastructure/
    ├── redis/            # RedisCartRepository — serializa o Cart como JSON, chave cart:{userId}
    ├── kafka/            # KafkaCartEventPublisher — publica cart.CHECKOUT
    ├── web/              # CartController, GlobalExceptionHandler, request/response records
    └── config/           # SecurityConfig, KafkaProducerConfig, OpenTelemetryAppenderConfig
```

O domínio não conhece Redis, Kafka nem Spring. As portas `CartRepository` e `CartEventPublisher` desacoplam a lógica de negócio da infraestrutura.

### Chave Redis

| Padrão | TTL | Conteúdo |
|---|---|---|
| `cart:{userId}` | 7 dias | JSON com `userId`, `items[]` e `updatedAt` |

O TTL é renovado a cada operação de escrita. O checkout apaga a chave imediatamente após publicar o evento.

---

## Autenticação

Todas as rotas requerem um **Bearer token JWT** emitido pelo Keycloak (realm `ecommerce`).

O token é validado via `spring-security-oauth2-resource-server` usando o JWKS endpoint do Keycloak. O `userId` utilizado como chave do carrinho é lido do claim `sub` do JWT — não é passado pelo cliente.

```bash
# Obter token via auth-service
curl -s -X POST http://localhost:8120/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "joao", "password": "senha123"}' | jq '.data.accessToken'
```

---

## API Reference

A aplicação sobe na porta `8140`. O Actuator fica na porta `8141`.

> Todos os exemplos assumem `TOKEN` como variável de ambiente com o access token JWT.

### GET /carts

Retorna o carrinho do usuário autenticado. Se não houver carrinho, retorna um carrinho vazio.

```bash
curl -s http://localhost:8140/carts \
  -H "Authorization: Bearer $TOKEN" | jq
```

**`200 OK`**

```json
{
  "data": {
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "items": [
      {
        "productId": "6650a1f3e4b09c2d3f8a1234",
        "name": "Tênis X",
        "price": 299.90,
        "quantity": 2
      }
    ],
    "updatedAt": "2026-06-22T22:00:00Z"
  }
}
```

---

### POST /carts/items

Adiciona um item ao carrinho. Se o `productId` já existir no carrinho, **soma** a quantidade (upsert).

```bash
curl -s -X POST http://localhost:8140/carts/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "6650a1f3e4b09c2d3f8a1234",
    "name": "Tênis X",
    "price": 299.90,
    "quantity": 2
  }' | jq
```

**`200 OK`** — retorna o carrinho atualizado.

---

### PUT /carts/items/{productId}

Substitui a quantidade de um item existente. Retorna `404` se o `productId` não estiver no carrinho.

```bash
curl -s -X PUT http://localhost:8140/carts/items/6650a1f3e4b09c2d3f8a1234 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"quantity": 5}' | jq
```

**`200 OK`** — retorna o carrinho atualizado.

---

### DELETE /carts/items/{productId}

Remove um item do carrinho. Se o `productId` não existir, o carrinho é retornado inalterado.

```bash
curl -s -X DELETE http://localhost:8140/carts/items/6650a1f3e4b09c2d3f8a1234 \
  -H "Authorization: Bearer $TOKEN" | jq
```

**`200 OK`** — retorna o carrinho atualizado.

---

### DELETE /carts

Limpa o carrinho inteiro.

```bash
curl -s -X DELETE http://localhost:8140/carts \
  -H "Authorization: Bearer $TOKEN"
```

**`204 No Content`**

---

### POST /carts/checkout

Publica o evento `cart.CHECKOUT` no Kafka e apaga o carrinho do Redis. Retorna `422` se o carrinho estiver vazio.

```bash
curl -s -X POST http://localhost:8140/carts/checkout \
  -H "Authorization: Bearer $TOKEN"
```

**`204 No Content`**

---

### Erros

| Status | Situação |
|---|---|
| `401 Unauthorized` | Token ausente, expirado ou inválido |
| `404 Not Found` | `productId` não encontrado no carrinho (PUT) |
| `422 Unprocessable Entity` | Campos inválidos (Bean Validation) ou carrinho vazio no checkout |
| `500 Internal Server Error` | Falha ao publicar evento no Kafka |

**Formato de erro:**

```json
{
  "errors": [
    { "field": "productId", "message": "Item not found in cart: prod-999" }
  ]
}
```

---

## Eventos Kafka

### Produzidos

| Tópico | Evento | Trigger |
|---|---|---|
| `cart` | `CHECKOUT` | `POST /carts/checkout` bem-sucedido |

**Formato da mensagem (JSON):**

```json
{
  "event": "CHECKOUT",
  "data": {
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "items": [
      {
        "productId": "6650a1f3e4b09c2d3f8a1234",
        "name": "Tênis X",
        "price": 299.90,
        "quantity": 2
      }
    ],
    "checkoutAt": "2026-06-22T22:00:00Z"
  }
}
```

A chave da mensagem é o `userId`. **Consumidor esperado:** `order-service`.

> [!IMPORTANT]
> O carrinho é apagado do Redis **somente após** a publicação do evento ser confirmada pelo broker. Se o Kafka estiver indisponível, o checkout falha com `500` e o carrinho é preservado.

---

## Como rodar localmente

**Pré-requisitos:** Java 21 e Docker.

### 1. Suba a infraestrutura

Na raiz do repositório:

```bash
docker compose up redis kafka keycloak -d
```

Serviços essenciais para o cart-service: Redis (`:6379`), Kafka (`:9092`) e Keycloak (`:8084`).

### 2. Inicie o serviço

```bash
# A partir de services/cart/
./mvnw spring-boot:run
```

### Variáveis de ambiente (com defaults para dev)

| Variável | Default | Descrição |
|---|---|---|
| `SERVER_PORT` | `8140` | Porta da API |
| `MANAGEMENT_PORT` | `8141` | Porta do Actuator |
| `REDIS_HOST` | `localhost` | Host do Redis |
| `REDIS_PORT` | `6379` | Porta do Redis |
| `KAFKA_BROKERS` | `localhost:9092` | Bootstrap servers do Kafka |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8084/realms/ecommerce` | Issuer URI para validação de JWT |
| `OTEL_ENDPOINT` | `http://localhost:4318` | Endpoint do OTel Collector |

### Actuator

```bash
curl http://localhost:8141/actuator/health
```

```json
{ "status": "UP" }
```

---

## Testes e qualidade

| Sufixo | Executor | Infraestrutura | Descrição |
|---|---|---|---|
| `*Test` | Surefire (`./mvnw test`) | Nenhuma | Testes unitários com Mockito |
| `*IT` | Failsafe (`./mvnw verify`) | Testcontainers | Testes de integração com Redis + Kafka reais |

```bash
# Testes unitários (sem Docker)
./mvnw test

# Build completo: unitários + integração + cobertura + análise estática
./mvnw verify
```

### Quality gates

| Ferramenta | Critério | Build falha se... |
|---|---|---|
| **JaCoCo** | Cobertura de linha e branch | LINE ou BRANCH < 80% |
| **SpotBugs** | Análise de bytecode | Qualquer bug detectado |
| **PMD** | Qualidade do código-fonte | Qualquer violação |

> Classes excluídas do JaCoCo: `CartApplication`, `*Configuration`, `*Properties`.

---

## CI

O workflow [`cart-quality.yml`](../../.github/workflows/cart-quality.yml) dispara em todo push e pull request que altere arquivos em `services/cart/`. Executa `./mvnw verify` no runner `ubuntu-latest` (Java 21 Temurin) e publica o relatório JaCoCo como artefato.
