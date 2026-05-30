# product-service

[![Quality](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/product-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/product-quality.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-8.0-green?logo=mongodb)

Microserviço de catálogo de produtos da plataforma de e-commerce. Responsável por criar e consultar produtos, armazenando-os no MongoDB.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (records, virtual threads) |
| Framework | Spring Boot 4.0 |
| Banco de dados | MongoDB |
| Observabilidade | OpenTelemetry + Grafana (Tempo · Loki · Prometheus) |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Qualidade | JaCoCo · SpotBugs · PMD |
| CI | GitHub Actions |

---

## Arquitetura

Organizado em **DDD** com separação em três camadas:

```
com.github.vagnerlg.product/
├── domain/           # Entidades, exceções de domínio e interface de repositório — sem dependências de framework
├── application/      # Casos de uso (ProductService) — orquestra o domínio
└── infrastructure/
    ├── persistence/  # Implementação MongoDB (MongoProductRepository, ProductDocument)
    └── web/          # Controladores REST (ProductController, GlobalExceptionHandler)
```

A camada de domínio não conhece Spring nem MongoDB. As dependências apontam sempre das camadas externas para as internas.

---

## API

A aplicação sobe na porta `8080`. O Actuator fica na porta `8081`.

### POST /products

Cria um novo produto. Retorna `409` se já existir um produto com o mesmo nome.

```bash
curl -s -X POST http://localhost:8080/products \
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

Busca um produto pelo ID. Retorna `404` se não encontrado.

```bash
curl -s http://localhost:8080/products/6650a1f3e4b09c2d3f8a1234 | jq
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
| `409 Conflict` | Produto com o mesmo nome já existe |
| `404 Not Found` | Produto não encontrado |
| `422 Unprocessable Entity` | Campos inválidos (Bean Validation) |

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

## Rodando localmente

**Pré-requisitos:** Java 21 e Docker.

**1. Suba o MongoDB:**

```bash
docker run -d --name mongo -p 27017:27017 mongo:8
```

**2. Inicie a aplicação:**

```bash
./mvnw spring-boot:run
```

Actuator disponível em `http://localhost:8081/actuator/health`.

---

## Testes e qualidade

```bash
# Testes unitários (sem Docker)
./mvnw test

# Build completo: unitários + integração + cobertura + análise estática
./mvnw verify
```

| Ferramenta | Threshold |
|---|---|
| JaCoCo | ≥ 80% line e branch coverage |
| SpotBugs | Zero violações |
| PMD | Zero violações |

Os testes de integração (`*IT`) usam **Testcontainers** — nenhuma infraestrutura local é necessária para rodá-los.

---

## CI

O workflow [`product-quality.yml`](../../.github/workflows/product-quality.yml) dispara em todo push e pull request que altere arquivos em `services/product/`. Executa `./mvnw verify` no runner `ubuntu-latest` e publica o relatório JaCoCo como artefato.
