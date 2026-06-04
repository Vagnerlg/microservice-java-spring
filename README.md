# microservice-java-spring

[![product-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/product-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/product-quality.yml)
[![search-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/search-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/search-quality.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)

Plataforma de e-commerce construída com **Java 21 + Spring Boot 4**, organizada em microserviços independentes. Cada serviço possui seu próprio banco de dados e se comunica via **Apache Kafka**.

O projeto está em construção progressiva — `product-service` e `search-service` já estão implementados. Os demais serão adicionados gradualmente.

---

## Stack

| | Tecnologias |
|---|---|
| **Linguagem & Framework** | Java 21 · Spring Boot 4 |
| **Bancos de dados** | MongoDB · PostgreSQL + Flyway |
| **Mensageria** | Apache Kafka (topic per aggregate) |
| **Observabilidade** | OpenTelemetry · Grafana (Tempo · Loki · Prometheus) |
| **Infraestrutura** | Kubernetes · Traefik · Docker |
| **Testes** | JUnit 5 · Mockito · Testcontainers |
| **CI/CD** | GitHub Actions · JaCoCo · SpotBugs · PMD |

---

## Serviços

| Serviço | Status | Banco | Descrição |
|---|---|---|---|
| [`product-service`](services/product/) | ✅ Implementado | MongoDB | Catálogo de produtos |
| `order-service` | 📋 Planejado | PostgreSQL | Pedidos, Saga, Outbox + Debezium |
| `inventory-service` | 📋 Planejado | PostgreSQL + Redis | Controle de estoque |
| `cart-service` | 📋 Planejado | Redis | Carrinho de compras |
| `user-service` | 📋 Planejado | PostgreSQL | Perfis de usuário |
| `auth-service` | 📋 Planejado | — | OAuth2/JWT via Keycloak |
| [`search-service`](services/search/) | ✅ Implementado | Elasticsearch | Busca — modelo CQRS read |
| `notification-service` | 📋 Planejado | — | Consumidor Kafka, sem HTTP |
| `report-service` | 📋 Planejado | MongoDB | Relatórios e analytics |

---

## product-service

Primeiro serviço da plataforma. Implementa criação e consulta de produtos com:

- **DDD** — separação em camadas `domain`, `application` e `infrastructure`
- **MongoDB** como banco de documentos
- **API REST** com envelope de resposta padronizado `{ data }` / `{ errors }`
- **Testcontainers** nos testes de integração — sem mocks de infraestrutura
- **Quality gates** em todo PR: cobertura ≥ 80% (JaCoCo), SpotBugs e PMD com zero violações

Consulte o [README do product-service](services/product/README.md) para detalhes da API, como rodar localmente e guia de testes.

---

## Arquitetura Kafka

Todos os eventos seguem o padrão **topic per aggregate** com envelope fixo:

```json
{ "event": "CREATED", "data": { ...campos do agregado... } }
```

| Tópico | Eventos |
|---|---|
| `product` | `CREATED` · `UPDATED` · `DELETED` |
| `order` | `CREATED` · `CANCELLED` |
| `cart` | `CHECKOUT` |
| `stock-reservation` | `RESERVED` · `UNAVAILABLE` · `RELEASED` |
| `stock-level` | `LOW` |
