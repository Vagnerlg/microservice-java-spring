# notification-service

[![notification-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/notification-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/notification-quality.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-consumer%20only-231F20?logo=apachekafka)

Serviço de notificações da plataforma de e-commerce. **Consumidor Kafka puro** — sem banco de dados, sem API REST. Escuta eventos de múltiplos tópicos e registra logs estruturados que alimentam o stack de observabilidade (Loki + Grafana).

---

## Índice

- [Stack](#stack)
- [Arquitetura interna (flat)](#arquitetura-interna-flat)
- [Eventos Kafka consumidos](#eventos-kafka-consumidos)
- [Como rodar localmente](#como-rodar-localmente)
- [Testes e qualidade](#testes-e-qualidade)
- [CI](#ci)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (records) |
| Framework | Spring Boot 4.0 |
| Banco de dados | Nenhum |
| Mensageria | Apache Kafka (consumer only) |
| Observabilidade | OpenTelemetry + Grafana (Tempo · Loki · Prometheus) |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Qualidade | JaCoCo · SpotBugs · PMD |
| CI | GitHub Actions |

---

## Arquitetura interna (flat)

O serviço não possui lógica de domínio — é um **sink de eventos**. A estrutura é plana, sem camadas de domínio ou aplicação.

```
com.github.vagnerlg.notification/
├── infrastructure/
│   └── kafka/          # Consumers + EventMessage records
│       ├── OrderEventConsumer       — order.CREATED, order.CANCELLED
│       ├── UserEventConsumer        — user.CREATED
│       ├── StockLevelEventConsumer  — stock-level.LOW
│       ├── OrderEventMessage
│       ├── UserEventMessage
│       └── StockLevelEventMessage
└── config/
    ├── KafkaConsumerConfig
    └── OpenTelemetryAppenderConfig
```

Cada consumer deserializa o payload com `ObjectMapper`, faz switch no campo `event` e emite um `log.info` ou `log.warn`. Erros de desserialização são capturados e logados sem propagar — o offset avança e o evento é descartado.

---

## Eventos Kafka consumidos

| Tópico | Evento | Log Level | Campos logados |
|---|---|---|---|
| `order` | `CREATED` | `INFO` | `orderId`, `userId`, `totalPrice` |
| `order` | `CANCELLED` | `INFO` | `orderId`, `userId` |
| `user` | `CREATED` | `INFO` | `username`, `name` |
| `stock-level` | `LOW` | `WARN` | `productId`, `currentQuantity` |

**Formato de `order.CREATED`:**

```json
{
  "event": "CREATED",
  "data": {
    "orderId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "totalPrice": 59.80,
    "createdAt": "2026-06-24T01:00:00Z"
  }
}
```

**Formato de `user.CREATED`:**

```json
{
  "event": "CREATED",
  "data": {
    "keycloakId": "kc-abc123",
    "username": "joao.silva",
    "name": "João Silva",
    "createdAt": "2026-06-24T01:00:00Z"
  }
}
```

**Formato de `stock-level.LOW`:**

```json
{
  "event": "LOW",
  "data": {
    "productId": "p-001",
    "currentQuantity": 2
  }
}
```

---

## Como rodar localmente

**Pré-requisitos:** Java 21 e Docker.

### 1. Suba a infraestrutura

Na raiz do repositório:

```bash
docker compose up -d
```

O `docker-compose.yml` sobe automaticamente Kafka. O notification-service não precisa de banco de dados.

### 2. Inicie o serviço

```bash
# A partir de services/notification/
./mvnw spring-boot:run
```

### Variáveis de ambiente (com defaults para dev)

| Variável | Default | Descrição |
|---|---|---|
| `SERVER_PORT` | `8170` | Porta do Actuator (sem API REST) |
| `MANAGEMENT_PORT` | `8171` | Porta do Actuator |
| `KAFKA_BROKERS` | `localhost:9092` | Bootstrap servers do Kafka |
| `OTEL_ENDPOINT` | `http://localhost:4318` | Endpoint do OTel Collector |

### Actuator

```bash
curl http://localhost:8171/actuator/health
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
| `*IT` | Failsafe (`./mvnw verify`) | Testcontainers | Context load com Kafka real |

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

> Classes excluídas do JaCoCo: `NotificationApplication`, `*Configuration`.

---

## CI

O workflow [`notification-quality.yml`](../../.github/workflows/notification-quality.yml) dispara em todo push e pull request que altere arquivos em `services/notification/`. Executa `./mvnw verify` no runner `ubuntu-latest` com Java 21 e publica o relatório JaCoCo como artefato.
