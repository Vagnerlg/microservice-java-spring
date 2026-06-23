# order-service

[![order-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/order-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/order-quality.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-consumer%20%2B%20producer-231F20?logo=apachekafka)

Serviço de pedidos da plataforma de e-commerce. Recebe o sinal de checkout via **Kafka** (`cart.CHECKOUT`), persiste o pedido no **PostgreSQL** e participa de uma Saga coreografada com o `inventory-service` para confirmar ou cancelar o pedido conforme a disponibilidade de estoque.

---

## Índice

- [Stack](#stack)
- [Arquitetura interna (DDD com hexagonal)](#arquitetura-interna-ddd-com-hexagonal)
- [Saga — fluxo de pedido](#saga--fluxo-de-pedido)
- [Eventos Kafka](#eventos-kafka)
- [API Reference](#api-reference)
- [Como rodar localmente](#como-rodar-localmente)
- [Testes e qualidade](#testes-e-qualidade)
- [CI](#ci)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (records, enums) |
| Framework | Spring Boot 4.0 |
| Banco de dados | PostgreSQL 17 + Flyway |
| Mensageria | Apache Kafka (consumer + producer) |
| Segurança | Spring Security OAuth2 Resource Server (JWT) |
| Observabilidade | OpenTelemetry + Grafana (Tempo · Loki · Prometheus) |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Qualidade | JaCoCo · SpotBugs · PMD |
| CI | GitHub Actions |

---

## Arquitetura interna (DDD com hexagonal)

O serviço segue **DDD com hexagonal** — o domínio define as portas `OrderRepository` e `OrderEventPublisher`; a infraestrutura fornece os adaptadores.

```
com.github.vagnerlg.order/
├── domain/               # Order, OrderItem, OrderStatus (enum), CreateOrderItem
│                         # OrderRepository (porta), OrderEventPublisher (porta)
│                         # exception/: OrderNotFoundException, OrderAccessDeniedException, OrderCancellationException
├── application/          # OrderService — casos de uso
└── infrastructure/
    ├── kafka/            # CartEventConsumer, StockReservationEventConsumer
    │                     # KafkaOrderEventPublisher, *EventMessage records
    ├── persistence/      # OrderEntity, OrderItemEntity, OrderJpaRepository, OrderPersistenceAdapter
    ├── web/              # OrderController, GlobalExceptionHandler, response records
    └── config/           # SecurityConfig, KafkaConsumerConfig, KafkaProducerConfig, OpenTelemetryAppenderConfig
```

### Status do pedido

```
PENDING ──► CONFIRMED   (stock.RESERVED recebido do inventory-service)
PENDING ──► CANCELLED   (stock.UNAVAILABLE recebido, ou cancelamento pelo usuário)
```

`OrderStatus` é um enum (`PENDING`, `CONFIRMED`, `CANCELLED`). O motivo do cancelamento é armazenado no campo `cancellationReason`.

---

## Saga — fluxo de pedido

A Saga é **coreografada via Kafka** — sem orquestrador central.

```
cart.CHECKOUT
      │
      ▼
[order-service]
  cria pedido PENDING
  publica order.CREATED
      │
      ▼
[inventory-service]
  verifica estoque
      │
  ┌───┴────────────────┐
  │ OK                 │ NOK
  ▼                    ▼
stock.RESERVED    stock.UNAVAILABLE
      │                 │
      ▼                 ▼
[order-service]   [order-service]
  CONFIRMED         CANCELLED
                  publica order.CANCELLED
                          │
                          ▼
                  [cart-service]
                   restaura carrinho
```

> **Tech debt:** Os eventos `order.CREATED` e `order.CANCELLED` são publicados diretamente no Kafka fora da transação (sem Outbox + Debezium). O Outbox Pattern será adicionado quando o `inventory-service` for implementado.

---

## Eventos Kafka

### Consumidos

| Tópico | Evento | Producer | Ação |
|---|---|---|---|
| `cart` | `CHECKOUT` | `cart-service` | Cria pedido `PENDING` |
| `stock-reservation` | `RESERVED` | `inventory-service` | Muda status para `CONFIRMED` |
| `stock-reservation` | `UNAVAILABLE` | `inventory-service` | Muda status para `CANCELLED` |

**Formato de `cart.CHECKOUT`:**

```json
{
  "event": "CHECKOUT",
  "data": {
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "items": [
      { "productId": "p-001", "name": "Widget", "price": 29.90, "quantity": 2 }
    ],
    "checkoutAt": "2026-06-22T01:00:00Z"
  }
}
```

**Formato de `stock-reservation`:**

```json
{ "event": "RESERVED",     "data": { "orderId": "f47ac10b-...", "reason": null } }
{ "event": "UNAVAILABLE",  "data": { "orderId": "f47ac10b-...", "reason": "Out of stock" } }
```

### Produzidos

| Tópico | Evento | Situação |
|---|---|---|
| `order` | `CREATED` | Pedido criado com sucesso |
| `order` | `CANCELLED` | Pedido cancelado (pelo sistema ou pelo usuário) |

**Formato:**

```json
{
  "event": "CREATED",
  "data": {
    "orderId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "totalPrice": 59.80,
    "createdAt": "2026-06-22T01:00:00Z"
  }
}
```

---

## API Reference

A aplicação sobe na porta `8150`. O Actuator fica na porta `8151`.

Todos os endpoints requerem Bearer token (JWT emitido pelo Keycloak). O `userId` é lido do claim `sub` do token.

> Pedidos são criados exclusivamente via evento Kafka (`cart.CHECKOUT`) — não há `POST /orders`.

---

### GET /orders

Lista os pedidos do usuário autenticado. Suporta paginação via parâmetros `page` e `size`.

```bash
curl -s "http://localhost:8150/orders?page=0&size=10" \
  -H "Authorization: Bearer <access_token>" | jq
```

**`200 OK`**

```json
{
  "data": {
    "content": [
      {
        "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "userId": "a1b2c3d4-...",
        "items": [
          { "productId": "p-001", "name": "Widget", "price": 29.90, "quantity": 2, "subtotal": 59.80 }
        ],
        "status": "PENDING",
        "totalPrice": 59.80,
        "cancellationReason": null,
        "createdAt": "2026-06-22T01:00:00Z",
        "updatedAt": "2026-06-22T01:00:00Z"
      }
    ],
    "page": { "number": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
  }
}
```

---

### GET /orders/{id}

Busca um pedido pelo ID. Retorna `403` se o pedido não pertencer ao usuário autenticado.

```bash
curl -s http://localhost:8150/orders/f47ac10b-58cc-4372-a567-0e02b2c3d479 \
  -H "Authorization: Bearer <access_token>" | jq
```

**`200 OK`** — mesmo formato de item da listagem acima.

---

### DELETE /orders/{id}

Cancela um pedido. Só é permitido se o pedido estiver em status `PENDING` e pertencer ao usuário autenticado.

```bash
curl -s -X DELETE http://localhost:8150/orders/f47ac10b-58cc-4372-a567-0e02b2c3d479 \
  -H "Authorization: Bearer <access_token>"
```

**`204 No Content`** — cancelamento realizado.

---

### Erros

| Status | Situação |
|---|---|
| `401 Unauthorized` | Token ausente, expirado ou inválido |
| `403 Forbidden` | Pedido pertence a outro usuário |
| `404 Not Found` | Pedido não encontrado |
| `422 Unprocessable Entity` | Tentativa de cancelar pedido que não está `PENDING` |

---

## Como rodar localmente

**Pré-requisitos:** Java 21 e Docker.

### 1. Suba a infraestrutura

Na raiz do repositório:

```bash
docker compose up -d
```

O `docker-compose.yml` sobe automaticamente PostgreSQL, Kafka, Keycloak e o stack de observabilidade. Para o order-service os serviços essenciais são PostgreSQL (`:5432`), Kafka (`:9092`) e Keycloak (`:8084`).

### 2. Inicie o serviço

```bash
# A partir de services/order/
./mvnw spring-boot:run
```

### Variáveis de ambiente (com defaults para dev)

| Variável | Default | Descrição |
|---|---|---|
| `SERVER_PORT` | `8150` | Porta da API |
| `MANAGEMENT_PORT` | `8151` | Porta do Actuator |
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `order` | Nome do banco |
| `DB_USERNAME` | `admin` | Usuário do banco |
| `DB_PASSWORD` | `admin` | Senha do banco |
| `KAFKA_BROKERS` | `localhost:9092` | Bootstrap servers do Kafka |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8084/realms/ecommerce` | Issuer URI para validação de JWT |
| `OTEL_ENDPOINT` | `http://localhost:4318` | Endpoint do OTel Collector |

### Actuator

```bash
curl http://localhost:8151/actuator/health
```

```json
{ "status": "UP" }
```

---

## Testes e qualidade

| Sufixo | Executor | Infraestrutura | Descrição |
|---|---|---|---|
| `*Test` | Surefire (`./mvnw test`) | Nenhuma | Testes unitários com Mockito |
| `*IT` | Failsafe (`./mvnw verify`) | Testcontainers | Testes de integração com PostgreSQL + Kafka reais |

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

> Classes excluídas do JaCoCo: `OrderApplication`, `*Configuration`, `*Properties`.

---

## CI

O workflow [`order-quality.yml`](../../.github/workflows/order-quality.yml) dispara em todo push e pull request que altere arquivos em `services/order/`. Executa `./mvnw verify` no runner `ubuntu-latest` com Java 21 e publica o relatório JaCoCo como artefato.
