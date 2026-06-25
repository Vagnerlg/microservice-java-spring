# inventory-service

[![inventory-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/inventory-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/inventory-quality.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-consumer%20%2B%20producer-231F20?logo=apachekafka)

Serviço de controle de estoque da plataforma de e-commerce. Consome eventos do **Kafka** para inicializar e gerenciar o estoque de produtos, participando da Saga coreografada com o `order-service` para reservar ou recusar estoque conforme disponibilidade.

---

## Índice

- [Stack](#stack)
- [Arquitetura interna (DDD com hexagonal)](#arquitetura-interna-ddd-com-hexagonal)
- [Saga — fluxo de reserva de estoque](#saga--fluxo-de-reserva-de-estoque)
- [Eventos Kafka](#eventos-kafka)
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
| Observabilidade | OpenTelemetry + Grafana (Tempo · Loki · Prometheus) |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Qualidade | JaCoCo · SpotBugs · PMD |
| CI | GitHub Actions |

---

## Arquitetura interna (DDD com hexagonal)

O serviço segue **DDD com hexagonal** — o domínio define as portas `StockRepository` e `StockReservationPublisher`; a infraestrutura fornece os adaptadores.

```
com.github.vagnerlg.inventory/
├── domain/               # Stock (record), StockRepository (porta), StockReservationPublisher (porta)
│                         # exception/: StockNotFoundException, InsufficientStockException
├── application/          # InventoryService — initializeStock, reserveStock, releaseStock
└── infrastructure/
    ├── kafka/            # ProductEventConsumer, OrderEventConsumer
    │                     # KafkaStockReservationPublisher, *EventMessage records
    ├── persistence/      # StockEntity, StockJpaRepository, StockPersistenceAdapter
    └── config/           # KafkaConsumerConfig, KafkaProducerConfig, OpenTelemetryAppenderConfig
```

### Modelo de estoque

```
Stock { id, productId, totalQuantity, reservedQuantity }
availableQuantity = totalQuantity - reservedQuantity
```

Estoque é inicializado com **10 unidades** ao receber `product.CREATED`. Ajustes manuais de quantidade serão suportados via API REST em uma fase futura.

---

## Saga — fluxo de reserva de estoque

A Saga é **coreografada via Kafka** — sem orquestrador central.

```
order.CREATED
      │
      ▼
[inventory-service]
  verifica estoque de cada item
      │
  ┌───┴────────────────────┐
  │ todos disponíveis      │ algum indisponível
  ▼                        ▼
reserva estoque       stock-reservation.UNAVAILABLE
stock-reservation.RESERVED      │
      │                         ▼
      ▼                   [order-service]
[order-service]             CANCELLED
  CONFIRMED               publica order.CANCELLED
                                  │
                                  ▼
                          [cart-service]
                           restaura carrinho
```

Quando um pedido é cancelado (`order.CANCELLED`), o inventory libera o `reservedQuantity` correspondente.

> **Tech debt:** Os eventos `stock-reservation.RESERVED` e `stock-reservation.UNAVAILABLE` são publicados diretamente no Kafka dentro da `@Transactional` (sem Outbox + Debezium). O Outbox Pattern será adicionado em uma fase futura para garantir entrega atômica.

> **Tech debt:** O evento `stock-level.LOW` (alerta de estoque baixo para o `notification-service`) não está implementado nesta fase.

---

## Eventos Kafka

### Consumidos

| Tópico | Evento | Producer | Ação |
|---|---|---|---|
| `product` | `CREATED` | `product-service` | Inicializa `Stock` com `totalQuantity = 10` |
| `order` | `CREATED` | `order-service` | Reserva estoque de todos os itens do pedido |
| `order` | `CANCELLED` | `order-service` | Libera `reservedQuantity` dos itens do pedido |

**Formato de `order.CREATED` / `order.CANCELLED`:**

```json
{
  "event": "CREATED",
  "data": {
    "orderId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "totalPrice": 59.80,
    "createdAt": "2026-06-24T01:00:00Z",
    "items": [
      { "productId": "p-001", "quantity": 2 }
    ]
  }
}
```

### Produzidos

| Tópico | Evento | Situação |
|---|---|---|
| `stock-reservation` | `RESERVED` | Todos os itens do pedido têm estoque disponível |
| `stock-reservation` | `UNAVAILABLE` | Algum item não tem estoque suficiente |

**Formato:**

```json
{ "event": "RESERVED",    "data": { "orderId": "f47ac10b-...", "reason": null } }
{ "event": "UNAVAILABLE", "data": { "orderId": "f47ac10b-...", "reason": "Insufficient stock for product p-001: requested=5, available=2" } }
```

---

## Como rodar localmente

**Pré-requisitos:** Java 21 e Docker.

### 1. Suba a infraestrutura

Na raiz do repositório:

```bash
docker compose up -d
```

O `docker-compose.yml` sobe automaticamente PostgreSQL e Kafka. Para o inventory-service os serviços essenciais são PostgreSQL (`:5432`) e Kafka (`:9092`).

### 2. Inicie o serviço

```bash
# A partir de services/inventory/
./mvnw spring-boot:run
```

### Variáveis de ambiente (com defaults para dev)

| Variável | Default | Descrição |
|---|---|---|
| `SERVER_PORT` | `8160` | Porta da API (Actuator only) |
| `MANAGEMENT_PORT` | `8161` | Porta do Actuator |
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `inventory` | Nome do banco |
| `DB_USERNAME` | `admin` | Usuário do banco |
| `DB_PASSWORD` | `admin` | Senha do banco |
| `KAFKA_BROKERS` | `localhost:9092` | Bootstrap servers do Kafka |
| `OTEL_ENDPOINT` | `http://localhost:4318` | Endpoint do OTel Collector |

### Actuator

```bash
curl http://localhost:8161/actuator/health
```

```json
{ "status": "UP" }
```

> Este serviço não expõe endpoints de negócio via HTTP — toda interação ocorre via Kafka.

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

> Classes excluídas do JaCoCo: `InventoryApplication`, `*Configuration`, `*Properties`.

---

## CI

O workflow [`inventory-quality.yml`](../../.github/workflows/inventory-quality.yml) dispara em todo push e pull request que altere arquivos em `services/inventory/`. Executa `./mvnw verify` no runner `ubuntu-latest` com Java 21 e publica o relatório JaCoCo como artefato.
