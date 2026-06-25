# microservice-java-spring

[![product-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/product-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/product-quality.yml)
[![search-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/search-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/search-quality.yml)
[![auth-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/auth-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/auth-quality.yml)
[![user-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/user-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/user-quality.yml)
[![cart-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/cart-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/cart-quality.yml)
[![order-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/order-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/order-quality.yml)
[![inventory-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/inventory-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/inventory-quality.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)

Plataforma de e-commerce construída com **Java 21 + Spring Boot 4**, organizada em microserviços independentes. Cada serviço possui seu próprio banco de dados e se comunica via **Apache Kafka**.

O projeto está em construção progressiva — `product-service`, `search-service`, `auth-service`, `user-service`, `cart-service`, `order-service` e `inventory-service` já estão implementados. Os demais serão adicionados gradualmente.

---

## Stack

| | Tecnologias |
|---|---|
| **Linguagem & Framework** | Java 21 · Spring Boot 4 |
| **Bancos de dados** | MongoDB · PostgreSQL + Flyway · Elasticsearch |
| **Cache / Identidade** | Redis · Keycloak 26 (OAuth2 / OIDC) |
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
| [`search-service`](services/search/) | ✅ Implementado | Elasticsearch | Busca — modelo CQRS read |
| [`auth-service`](services/auth/) | ✅ Implementado | — (Keycloak + Redis) | OAuth2/JWT, blacklist de tokens |
| [`user-service`](services/user/) | ✅ Implementado | PostgreSQL | Perfis de usuário |
| [`cart-service`](services/cart/) | ✅ Implementado | Redis | Carrinho de compras |
| [`order-service`](services/order/) | ✅ Implementado | PostgreSQL | Pedidos, Saga coreografada |
| [`inventory-service`](services/inventory/) | ✅ Implementado | PostgreSQL | Controle de estoque, Saga coreografada |
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

## search-service

Serviço de busca da plataforma. Implementa o modelo de leitura CQRS: consome eventos do Kafka e mantém o índice Elasticsearch atualizado.

- **DDD com hexagonal** — porta `ProductRepository` desacopla o domínio do Elasticsearch
- **Kafka consumer** — consome `product.CREATED`, `UPDATED` e `DELETED` do tópico `product`
- **Elasticsearch** como store de leitura — busca full-text paginada com filtro por categoria
- **Sem banco transacional próprio** — read model puro, sem HTTP de escrita
- **Testcontainers** nos testes de integração com Elasticsearch e Kafka reais

Consulte o [README do search-service](services/search/README.md) para detalhes da API, contrato de eventos e como rodar localmente.

---

## auth-service

Serviço de autenticação da plataforma. Delega identidade e sessões ao Keycloak, sem armazenar usuários localmente.

- **DDD sem hexagonal** — camada `application` chama `infrastructure` diretamente
- **Keycloak 26** como IdP: registro via Admin API, login/refresh/logout via ROPC
- **Redis** para blacklist de JTI — invalida access tokens sem aguardar expiração natural
- **Kafka** publica `user.CREATED` no tópico `user` a cada novo cadastro, consumido pelo `user-service`
- **Testcontainers** nos testes de integração com Keycloak, Redis e Kafka reais

Consulte o [README do auth-service](services/auth/README.md) para detalhes da API, integração Keycloak e variáveis de ambiente.

---

## user-service

Serviço de perfis de usuário da plataforma. Não gerencia autenticação — delega isso ao `auth-service`.

- **DDD com hexagonal** — porta `UserRepository` no domínio, adaptada por `UserPersistenceAdapter` na infraestrutura
- **Kafka consumer** — consome `user.CREATED` do tópico `user` publicado pelo `auth-service` a cada novo cadastro
- **PostgreSQL** como store de perfis — schema gerenciado por Flyway (`V1__create_users.sql`)
- **`GET /users/me`** — único endpoint REST; requer JWT do Keycloak, lê `keycloakId` do claim `sub`
- **Idempotente** — eventos duplicados com o mesmo `keycloakId` são descartados silenciosamente
- **Testcontainers** nos testes de integração com PostgreSQL e Kafka reais

Consulte o [README do user-service](services/user/README.md) para detalhes da API, variáveis de ambiente e como rodar localmente.

---

## cart-service

Microserviço de carrinho de compras da plataforma. Armazena o carrinho de cada usuário no Redis e coordena o início do fluxo de pedidos via Kafka.

- **DDD** — portas `CartRepository` e `CartEventPublisher` isolam o domínio do Redis e do Kafka
- **Redis** como storage — chave `cart:{userId}` com TTL de 7 dias; upsert de item soma quantidade ao existente
- **JWT obrigatório** — resource server OAuth2; `userId` extraído do claim `sub` do token Keycloak
- **6 endpoints REST** — get, add item, update item, remove item, clear e checkout
- **`POST /carts/checkout`** publica `cart.CHECKOUT` no tópico `cart` e apaga o carrinho atomicamente
- **Testcontainers** nos testes de integração com Redis e Kafka reais

Consulte o [README do cart-service](services/cart/README.md) para detalhes da API, contrato do evento e como rodar localmente.

---

## order-service

Serviço de pedidos da plataforma. Orquestra o ciclo de vida do pedido desde o checkout até a confirmação de estoque.

- **DDD com hexagonal** — portas `OrderRepository` e `OrderEventPublisher` isolam o domínio do PostgreSQL e do Kafka
- **Kafka consumer** — consome `cart.CHECKOUT` para criar pedidos e `stock-reservation.RESERVED/UNAVAILABLE` para avançar o status via Saga
- **PostgreSQL** como store transacional — schema gerenciado por Flyway (`V1__create_orders.sql`, `V2__create_order_items.sql`)
- **Saga coreografada** — `PENDING → CONFIRMED` ao receber `stock.RESERVED`; `PENDING → CANCELLED` ao receber `stock.UNAVAILABLE` ou por ação do usuário
- **3 endpoints REST** — listagem paginada, consulta por ID e cancelamento pelo usuário (todos exigem JWT)
- **Testcontainers** nos testes de integração com PostgreSQL e Kafka reais

Consulte o [README do order-service](services/order/README.md) para detalhes da API, fluxo da Saga e como rodar localmente.

---

## inventory-service

Serviço de controle de estoque da plataforma. Participa da Saga coreografada com o `order-service` para reservar ou recusar estoque.

- **DDD com hexagonal** — portas `StockRepository` e `StockReservationPublisher` isolam o domínio do PostgreSQL e do Kafka
- **Kafka consumer** — consome `product.CREATED` para inicializar estoque (10 unidades) e `order.CREATED` / `order.CANCELLED` para reservar e liberar estoque
- **PostgreSQL** como store transacional — schema gerenciado por Flyway (`V1__create_stock.sql`)
- **Sem HTTP API** — toda interação ocorre via Kafka; Actuator disponível na porta `8161`
- **Saga coreografada** — publica `stock-reservation.RESERVED` ou `stock-reservation.UNAVAILABLE` conforme disponibilidade de cada item do pedido
- **Testcontainers** nos testes de integração com PostgreSQL e Kafka reais

Consulte o [README do inventory-service](services/inventory/README.md) para detalhes dos eventos, fluxo da Saga e como rodar localmente.

---

## Arquitetura HTTP

Todos os serviços seguem os mesmos padrões de contrato REST.

### Envelope de resposta

**Sucesso** — corpo sempre envolto em `data`:

```json
{ "data": { ...campos do recurso... } }
```

**Erro** — lista de erros com `field` (campo inválido, ou `null` para erros de negócio):

```json
{
  "errors": [
    { "field": "username", "message": "não deve estar em branco" },
    { "field": null,       "message": "User already exists: joao" }
  ]
}
```

### Status HTTP

| Status | Situação |
|---|---|
| `200 OK` | Leitura ou operação bem-sucedida com corpo |
| `201 Created` | Recurso criado com sucesso |
| `204 No Content` | Operação bem-sucedida sem corpo (ex: logout) |
| `401 Unauthorized` | Credenciais inválidas ou token expirado |
| `404 Not Found` | Recurso não encontrado |
| `409 Conflict` | Conflito de unicidade (ex: username já existe) |
| `422 Unprocessable Entity` | Falha de Bean Validation — campos inválidos |
| `500 Internal Server Error` | Erro interno (ex: falha ao publicar no Kafka) |

---

## Arquitetura Kafka

Todos os eventos seguem o padrão **topic per aggregate** com envelope fixo:

```json
{ "event": "CREATED", "data": { ...campos do agregado... } }
```

| Tópico | Eventos |
|---|---|
| `user` | `CREATED` |
| `product` | `CREATED` · `UPDATED` · `DELETED` |
| `order` | `CREATED` · `CANCELLED` |
| `cart` | `CHECKOUT` |
| `stock-reservation` | `RESERVED` · `UNAVAILABLE` · `RELEASED` |
| `stock-level` | `LOW` |
