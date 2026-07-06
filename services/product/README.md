# product-service

[![Quality](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/product-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/product-quality.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-8.0-green?logo=mongodb)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-black?logo=apachekafka)

Microserviço de catálogo de produtos da plataforma de e-commerce. Responsável por criar e consultar produtos, armazenando-os no MongoDB, e publicar eventos de domínio no Kafka.

---

## Índice

- [Stack](#stack)
- [Arquitetura interna (DDD)](#arquitetura-interna-ddd)
- [API Reference](#api-reference)
- [Eventos Kafka](#eventos-kafka)
- [Como rodar localmente](#como-rodar-localmente)
- [Testes e qualidade](#testes-e-qualidade)
- [CI](#ci)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (records, virtual threads) |
| Framework | Spring Boot 4.0 |
| Banco de dados | MongoDB |
| Mensageria | Apache Kafka |
| Segurança | Spring Security OAuth2 Resource Server (JWT) |
| Observabilidade | OpenTelemetry + Grafana (Tempo · Loki · Prometheus) |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Qualidade | JaCoCo · SpotBugs · PMD |
| CI | GitHub Actions |

---

## Arquitetura interna (DDD)

Organizado em **DDD** com separação em três camadas:

```
com.github.vagnerlg.product/
├── domain/           # Entidades, exceções de domínio e interface de repositório — sem dependências de framework
├── application/      # Casos de uso (ProductService) — orquestra o domínio
└── infrastructure/
    ├── persistence/  # Implementação MongoDB (MongoProductRepository, ProductDocument)
    └── web/          # Controladores REST (ProductController, GlobalExceptionHandler)
                       # security/SecurityConfiguration — exige ROLE_ADMIN para escrita
```

A camada de domínio não conhece Spring nem MongoDB. As dependências apontam sempre das camadas externas para as internas.

---

## API Reference

A aplicação sobe na porta `8101`. O Actuator fica na porta `8102`.

### Segurança

Leituras (`GET /products/**`) são públicas. Qualquer outra rota (`POST`, `PUT`, `DELETE`) exige Bearer token JWT (emitido pelo Keycloak) com a role `ADMIN` no claim `realm_access.roles`.

### POST /products

Cria um novo produto. Requer JWT com role `ADMIN`. Retorna `409` se já existir um produto com o mesmo nome.

```bash
curl -s -X POST http://localhost:8101/products \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Teclado Mecânico",
    "description": "Switch Cherry MX Red, layout ABNT2",
    "price": 349.90,
    "category": "Periféricos"
  }' | jq
```

```json
{
  "data": {
    "id": "6650a1f3e4b09c2d3f8a1234",
    "name": "Teclado Mecânico",
    "description": "Switch Cherry MX Red, layout ABNT2",
    "price": 349.90,
    "category": "Periféricos",
    "createdAt": "2025-01-15T14:30:00Z",
    "updatedAt": "2025-01-15T14:30:00Z"
  }
}
```

### GET /products/{id}

Busca um produto pelo ID. Endpoint público — não requer JWT. Retorna `404` se não encontrado.

```bash
curl -s http://localhost:8101/products/6650a1f3e4b09c2d3f8a1234 | jq
```

```json
{
  "data": {
    "id": "6650a1f3e4b09c2d3f8a1234",
    "name": "Teclado Mecânico",
    "description": "Switch Cherry MX Red, layout ABNT2",
    "price": 349.90,
    "category": "Periféricos",
    "createdAt": "2025-01-15T14:30:00Z",
    "updatedAt": "2025-01-15T14:30:00Z"
  }
}
```

### Erros

| Status | Situação |
|---|---|
| `401 Unauthorized` | Token ausente, expirado ou inválido em rota de escrita |
| `403 Forbidden` | Token válido mas sem a role `ADMIN` |
| `409 Conflict` | Produto com o mesmo nome já existe |
| `404 Not Found` | Produto não encontrado |
| `422 Unprocessable Entity` | Campos inválidos (Bean Validation) |
| `500 Internal Server Error` | Falha ao publicar evento no Kafka |

Exemplo de resposta de erro de validação:

```json
{
  "errors": [
    { "field": "name", "message": "não deve estar em branco" },
    { "field": "price", "message": "deve ser maior que 0.01" }
  ]
}
```

---

## Eventos Kafka

### Produzidos

Ao criar um produto com sucesso, o serviço publica um evento no tópico `product`.

| Tópico | Evento | Trigger |
|---|---|---|
| `product` | `CREATED` | `POST /products` bem-sucedido |

**Formato da mensagem (JSON):**

```json
{
  "event": "CREATED",
  "data": {
    "id": "6650a1f3e4b09c2d3f8a1234",
    "name": "Teclado Mecânico",
    "description": "Switch Cherry MX Red, layout ABNT2",
    "price": 349.90,
    "category": "Periféricos",
    "createdAt": "2025-01-15T14:30:00Z",
    "updatedAt": "2025-01-15T14:30:00Z"
  }
}
```

A chave da mensagem é o `id` do produto (String). Não há headers de tipo — o campo `event` identifica o tipo do evento.

**Consumidores esperados:** `inventory-service` (reserva de estoque), `search-service` (índice de busca).

---

## Como rodar localmente

**Pré-requisitos:** Java 21 e Docker.

### 1. Suba a infraestrutura

Na raiz do repositório:

```bash
docker compose up -d
```

Para o product-service os serviços essenciais são MongoDB (`:27017`), Kafka (`:9092`) e Keycloak (`:8084`).

### 2. Inicie o serviço

```bash
# A partir de services/product/
./mvnw spring-boot:run
```

### Variáveis de ambiente (com defaults para dev)

| Variável | Default | Descrição |
|---|---|---|
| `SERVER_PORT` | `8101` | Porta da API |
| `MANAGEMENT_PORT` | `8102` | Porta do Actuator |
| `MONGO_HOST` | `localhost` | Host do MongoDB |
| `MONGO_PORT` | `27017` | Porta do MongoDB |
| `MONGO_USERNAME` | `admin` | Usuário do MongoDB |
| `MONGO_PASSWORD` | `admin` | Senha do MongoDB |
| `MONGO_DATABASE` | `product` | Nome do banco |
| `MONGO_AUTH_DATABASE` | `admin` | Banco de autenticação do MongoDB |
| `KAFKA_BROKERS` | `localhost:9092` | Bootstrap servers do Kafka |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8084/realms/ecommerce` | Issuer URI para validação de JWT |

### Actuator

```bash
curl http://localhost:8102/actuator/health
```

```json
{ "status": "UP" }
```

---

## Testes e qualidade

| Sufixo | Executor | Infraestrutura | Descrição |
|---|---|---|---|
| `*Test` | Surefire (`./mvnw test`) | Nenhuma | Testes unitários com Mockito |
| `*IT` | Failsafe (`./mvnw verify`) | Testcontainers | Testes de integração com MongoDB + Kafka reais |

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

> Classes excluídas do JaCoCo: `MainApplication`, `*Configuration`, `*Properties`.

Os testes de integração (`*IT`) usam **Testcontainers** — nenhuma infraestrutura local é necessária para rodá-los.

---

## CI

O workflow [`product-quality.yml`](../../.github/workflows/product-quality.yml) dispara em todo push e pull request que altere arquivos em `services/product/`. Executa `./mvnw verify` no runner `ubuntu-latest` e publica o relatório JaCoCo como artefato.
