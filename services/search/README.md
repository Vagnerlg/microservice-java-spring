# search-service

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.x-005571?logo=elasticsearch)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-consumer-231F20?logo=apachekafka)
![JaCoCo](https://img.shields.io/badge/coverage-≥80%25-brightgreen?logo=jacoco)
![Testcontainers](https://img.shields.io/badge/tests-Testcontainers-blue?logo=docker)
![CI](https://github.com/Vagnerlg/java-spring-boot-base-project/actions/workflows/quality.yml/badge.svg)

Serviço de busca de produtos da plataforma de e-commerce. Implementa o modelo de leitura CQRS: consome eventos do tópico Kafka `product`, mantém o índice Elasticsearch atualizado e expõe uma API de busca full-text paginada.

---

## Índice

- [Visão geral](#visão-geral)
- [Arquitetura interna (DDD)](#arquitetura-interna-ddd)
- [Contrato de eventos Kafka](#contrato-de-eventos-kafka)
- [API Reference](#api-reference)
- [Índice Elasticsearch](#índice-elasticsearch)
- [Como rodar localmente](#como-rodar-localmente)
- [Testes e qualidade](#testes-e-qualidade)
- [Observabilidade](#observabilidade)

---

## Visão geral

O `search-service` é um consumidor puro: não possui banco de dados transacional próprio nem publica eventos. Sua única responsabilidade é manter o índice de busca sincronizado com o catálogo de produtos e responder consultas full-text com baixa latência.

```
┌─────────────────┐   product.CREATED / UPDATED / DELETED   ┌──────────────────┐
│  product-service │ ──────────────── Kafka ────────────────► │  search-service  │
└─────────────────┘          (tópico: product)                │                  │
                                                              │  ┌────────────┐  │
                                                              │  │    Kafka   │  │
                                                              │  │  Consumer  │  │
                                                              │  └─────┬──────┘  │
                                                              │        │ index / │
                                                              │        │ delete  │
                                                              │  ┌─────▼──────┐  │
                                                              │  │Elasticsearch│  │
                                                              │  │  products  │  │
                                                              │  └─────┬──────┘  │
                                                              │        │ search  │
                                                              │  ┌─────▼──────┐  │
                                                              │  │  REST API  │  │
                                                              │  │GET /search │  │
                                                              │  └────────────┘  │
                                                              └──────────────────┘
                                                                       │
                                                                       ▼
                                                               clientes / BFF
```

---

## Arquitetura interna (DDD)

O serviço segue os princípios de **Domain-Driven Design**, separando claramente domínio, aplicação e infraestrutura:

```
com.github.vagnerlg.search/
├── domain/
│   ├── Product.java              # Entidade de domínio (record imutável)
│   └── ProductRepository.java    # Porta de saída (interface)
│
├── application/
│   └── ProductSearchService.java # Casos de uso: index, delete, search
│
└── infrastructure/
    ├── elasticsearch/
    │   ├── ProductDocument.java              # Mapeamento do índice ES
    │   └── ElasticsearchProductRepository.java  # Adaptador da porta de saída
    ├── kafka/
    │   ├── ProductEventConsumer.java         # Adaptador de entrada (Kafka listener)
    │   └── ProductEventMessage.java          # Deserialização do envelope de evento
    └── web/
        ├── ProductSearchController.java      # Adaptador de entrada (REST)
        └── ProductSearchResponse.java        # DTO de resposta
```

**Fluxo de um evento:**
1. `ProductEventConsumer` recebe o `ConsumerRecord` do Kafka
2. Deserializa o envelope `{ event, data }` para `ProductEventMessage`
3. Delega ao `ProductSearchService` (`index` ou `delete`)
4. O serviço chama a porta `ProductRepository`
5. `ElasticsearchProductRepository` executa a operação no índice

**Fluxo de uma busca:**
1. `ProductSearchController` recebe `GET /products/search`
2. Delega ao `ProductSearchService.search(query, category, pageable)`
3. O repositório executa `multi_match` em `name`+`description` com filtro opcional por `category`
4. Retorna `Page<ProductSearchResponse>` ao cliente

---

## Contrato de eventos Kafka

**Tópico:** `product`  
**Formato:** envelope JSON `{ "event": "<TIPO>", "data": { ...campos... } }`

### Tipos de evento

| Evento | Ação no índice |
|---|---|
| `CREATED` | Indexa o documento |
| `UPDATED` | Reindexe o documento (upsert) |
| `DELETED` | Remove o documento pelo `id` |

### Payload

```json
{
  "event": "CREATED",
  "data": {
    "id": "64f1a2b3c4d5e6f7a8b9c0d1",
    "name": "Tênis Running Pro",
    "description": "Tênis de corrida com amortecimento avançado",
    "price": 349.90,
    "category": "calcados",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
}
```

Eventos com tipo desconhecido são ignorados com log `WARN` — o serviço não falha nem comita o offset para garantir reprocessamento.

---

## API Reference

### `GET /products/search`

Busca full-text paginada no catálogo de produtos.

**Parâmetros**

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `q` | string | sim | Texto livre — busca em `name` e `description` |
| `category` | string | não | Filtro exato por categoria (`keyword`) |
| `page` | int | não | Número da página (0-based, padrão: 0) |
| `size` | int | não | Itens por página (padrão: 20) |
| `sort` | string | não | Ex: `price,asc` ou `name,desc` |

**Exemplo de requisição**

```bash
curl "http://localhost:8080/products/search?q=tênis&category=calcados&page=0&size=5"
```

**Exemplo de resposta** `200 OK`

```json
{
  "content": [
    {
      "id": "64f1a2b3c4d5e6f7a8b9c0d1",
      "name": "Tênis Running Pro",
      "description": "Tênis de corrida com amortecimento avançado",
      "price": 349.90,
      "category": "calcados",
      "createdAt": "2024-01-15T10:30:00Z",
      "updatedAt": "2024-01-15T10:30:00Z"
    }
  ],
  "page": {
    "size": 5,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## Índice Elasticsearch

**Nome:** `products`

| Campo | Tipo ES | Comportamento |
|---|---|---|
| `id` | `_id` | Identificador do documento |
| `name` | `text` | Analisado — busca full-text |
| `description` | `text` | Analisado — busca full-text |
| `price` | `double` | Numérico — suporta ordenação e range |
| `category` | `keyword` | Não analisado — filtro exato |
| `createdAt` | `date` (ISO 8601) | Suporta ordenação temporal |
| `updatedAt` | `date` (ISO 8601) | Suporta ordenação temporal |

**Estratégia de busca:**

- `multi_match` em `name` + `description` para o parâmetro `q`
- `term` filter em `category` quando informado (executa no contexto `bool.filter`, não afeta relevância)

---

## Como rodar localmente

### Pré-requisitos

- Java 21
- Docker Desktop

### 1. Suba a infraestrutura

Na raiz do repositório:

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

Inicia: Elasticsearch, Kafka, Zookeeper e o stack de observabilidade (OTel Collector, Grafana, Loki, Prometheus).

### 2. Inicie o serviço

```bash
# A partir de services/search/
./mvnw spring-boot:run
```

Com logs em JSON estruturado (ECS):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Portas

| Serviço | Porta |
|---|---|
| API REST | `8080` |
| Actuator (health, metrics) | `8081` |
| Elasticsearch | `9200` |
| Kafka | `9092` |
| Grafana | `3000` (admin/admin) |

### Actuator

```bash
curl http://localhost:8081/actuator/health
```

```json
{ "status": "UP" }
```

---

## Testes e qualidade

### Estrutura

| Sufixo | Executor | Infraestrutura | Descrição |
|---|---|---|---|
| `*Test` | Surefire (`./mvnw test`) | Nenhuma | Testes unitários com Mockito |
| `*IT` | Failsafe (`./mvnw verify`) | Testcontainers | Testes de integração com ES + Kafka reais |

Os testes de integração sobem containers Docker reais via Testcontainers — sem mocks de infraestrutura.

### Comandos

```bash
# Testes unitários (sem Docker)
./mvnw test

# Build completo: unitários + integração + cobertura + análise estática
./mvnw verify

# Relatório de cobertura
./mvnw test jacoco:report
# → target/site/jacoco/index.html
```

### Quality gates (executados em `./mvnw verify`)

| Ferramenta | Critério | Build falha se... |
|---|---|---|
| **JaCoCo** | Cobertura de linha e branch | LINE ou BRANCH < 80% |
| **SpotBugs** | Análise de bytecode | Qualquer bug detectado |
| **PMD** | Qualidade do código-fonte | Qualquer violação |

> Classes excluídas do JaCoCo: `*Application`, `*Configuration`, `*Properties`.

---

## Observabilidade

O serviço exporta os três sinais via **OTLP HTTP** para o OTel Collector:

```
search-service (:8081)
  │
  └─ OTLP HTTP (:4318) ──► OTel Collector
                                ├─ Traces   ──► Grafana Tempo  (:3200)
                                ├─ Métricas ──► Prometheus     (:9090)
                                └─ Logs     ──► Loki           (:3100)
                                                     │
                                               Grafana (:3000)
```

Cada log carrega automaticamente `traceId` e `spanId` — é possível navegar de um trace no Grafana Tempo diretamente para os logs correlacionados no Loki.

Sampling configurado em 100% para desenvolvimento. Reduzir `management.tracing.sampling.probability` em produção.
