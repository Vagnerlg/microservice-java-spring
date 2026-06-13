# user-service

[![user-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/user-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/user-quality.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-consumer-231F20?logo=apachekafka)

Serviço de perfis de usuário da plataforma de e-commerce. Não gerencia autenticação — isso é responsabilidade do `auth-service`. Consome o evento `user.CREATED` do **Kafka** e persiste o perfil no **PostgreSQL**. Expõe um único endpoint REST para o usuário autenticado consultar seus próprios dados.

---

## Índice

- [Stack](#stack)
- [Arquitetura interna (DDD com hexagonal)](#arquitetura-interna-ddd-com-hexagonal)
- [Evento Kafka consumido](#evento-kafka-consumido)
- [API Reference](#api-reference)
- [Como rodar localmente](#como-rodar-localmente)
- [Testes e qualidade](#testes-e-qualidade)
- [CI](#ci)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (records) |
| Framework | Spring Boot 4.0 |
| Banco de dados | PostgreSQL 17 + Flyway |
| Mensageria | Apache Kafka (consumer) |
| Segurança | Spring Security OAuth2 Resource Server (JWT) |
| Observabilidade | OpenTelemetry + Grafana (Tempo · Loki · Prometheus) |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Qualidade | JaCoCo · SpotBugs · PMD |
| CI | GitHub Actions |

---

## Arquitetura interna (DDD com hexagonal)

O serviço segue **DDD com hexagonal** — o domínio define a porta `UserRepository`; a infraestrutura fornece o adaptador `UserPersistenceAdapter`:

```
com.github.vagnerlg.user/
├── domain/               # User (record), UserRepository (porta), UserNotFoundException
├── application/          # UserService — casos de uso (create, findByKeycloakId)
└── infrastructure/
    ├── kafka/            # UserEventConsumer (@KafkaListener no tópico user)
    ├── persistence/      # UserEntity, UserJpaRepository, UserPersistenceAdapter
    ├── web/              # UserController, GlobalExceptionHandler, response records
    └── config/           # SecurityConfig, OpenTelemetryAppenderConfig
```

### Fluxo de criação de usuário

```
auth-service
  └─► Kafka topic: user → CREATED
                              │
                     UserEventConsumer
                              │
                         UserService.create()
                              │
                     UserPersistenceAdapter
                              │
                        PostgreSQL (users)
```

O consumer é **idempotente**: eventos duplicados com o mesmo `keycloakId` são descartados silenciosamente.

---

## Evento Kafka consumido

| Tópico | Evento | Producer |
|---|---|---|
| `user` | `CREATED` | `auth-service` |

**Formato da mensagem (JSON):**

```json
{
  "event": "CREATED",
  "data": {
    "keycloakId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "username": "joao",
    "name": "João Silva",
    "createdAt": "2026-06-11T23:00:00Z"
  }
}
```

O evento é publicado pelo `auth-service` ao concluir o `POST /auth/register` com sucesso.

---

## API Reference

A aplicação sobe na porta `8130`. O Actuator fica na porta `8131`.

### GET /users/me

Retorna o perfil do usuário autenticado. Requer Bearer token (JWT emitido pelo Keycloak). O `keycloakId` é lido do claim `sub` do token.

```bash
curl -s http://localhost:8130/users/me \
  -H "Authorization: Bearer <access_token>" | jq
```

**`200 OK`**

```json
{
  "data": {
    "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "username": "joao",
    "name": "João Silva",
    "createdAt": "2026-06-11T23:00:00Z"
  }
}
```

### Erros

| Status | Situação |
|---|---|
| `401 Unauthorized` | Token ausente, expirado ou inválido |
| `404 Not Found` | Usuário autenticado no Keycloak mas evento `CREATED` ainda não processado |

---

## Como rodar localmente

**Pré-requisitos:** Java 21 e Docker.

### 1. Suba a infraestrutura

Na raiz do repositório:

```bash
docker compose up -d
```

O `docker-compose.yml` sobe automaticamente PostgreSQL, Kafka, Keycloak e o stack de observabilidade. Para o user-service os serviços essenciais são PostgreSQL (`:5432`), Kafka (`:9092`) e Keycloak (`:8084`).

### 2. Inicie o serviço

```bash
# A partir de services/user/
./mvnw spring-boot:run
```

### Variáveis de ambiente (com defaults para dev)

| Variável | Default | Descrição |
|---|---|---|
| `SERVER_PORT` | `8130` | Porta da API |
| `MANAGEMENT_PORT` | `8131` | Porta do Actuator |
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `user` | Nome do banco |
| `DB_USERNAME` | `admin` | Usuário do banco |
| `DB_PASSWORD` | `admin` | Senha do banco |
| `KAFKA_BROKERS` | `localhost:9092` | Bootstrap servers do Kafka |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8084/realms/ecommerce` | Issuer URI para validação de JWT |
| `OTEL_ENDPOINT` | `http://localhost:4318` | Endpoint do OTel Collector |

### Actuator

```bash
curl http://localhost:8131/actuator/health
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

> Classes excluídas do JaCoCo: `UserApplication`, `*Configuration`, `*Properties`.

---

## CI

O workflow [`user-quality.yml`](../../.github/workflows/user-quality.yml) dispara em todo push e pull request que altere arquivos em `services/user/`. Executa `./mvnw verify` no runner `ubuntu-latest` e publica o relatório JaCoCo como artefato.
