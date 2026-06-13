# auth-service

[![auth-service CI](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/auth-quality.yml/badge.svg)](https://github.com/Vagnerlg/microservice-java-spring/actions/workflows/auth-quality.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)
![Keycloak](https://img.shields.io/badge/Keycloak-26.x-4d4d4d?logo=keycloak)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-producer-231F20?logo=apachekafka)

Serviço de autenticação da plataforma de e-commerce. Delega identidade e tokens ao **Keycloak**, usa **Redis** para blacklist de JTI e publica o evento `user.CREATED` no **Kafka** a cada novo cadastro.

---

## Índice

- [Stack](#stack)
- [Arquitetura interna (DDD)](#arquitetura-interna-ddd)
- [Integração Keycloak](#integração-keycloak)
- [API Reference](#api-reference)
- [Evento Kafka](#evento-kafka)
- [Como rodar localmente](#como-rodar-localmente)
- [Testes e qualidade](#testes-e-qualidade)
- [Limitações e próximos passos](#limitações-e-próximos-passos)
- [CI](#ci)

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (records, virtual threads) |
| Framework | Spring Boot 4.0 |
| Identidade | Keycloak 26.x (OAuth2 / OIDC) |
| Cache / Blacklist | Redis 7 |
| Mensageria | Apache Kafka |
| HTTP client | Spring RestClient |
| Observabilidade | OpenTelemetry + Grafana (Tempo · Loki · Prometheus) |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Qualidade | JaCoCo · SpotBugs · PMD |
| CI | GitHub Actions |

---

## Arquitetura interna (DDD)

O serviço segue **DDD sem hexagonal** — as camadas de aplicação chamam a infraestrutura diretamente, sem interfaces de porta no domínio:

```
com.github.vagnerlg.auth/
├── domain/               # Entidades (User, AuthToken) e exceções de domínio
├── application/          # Casos de uso — orquestram domínio e infraestrutura
│   ├── RegisterUserService
│   ├── LoginService
│   ├── RefreshTokenService
│   └── LogoutService
└── infrastructure/
    ├── keycloak/         # Admin API + OIDC token endpoint (RestClient)
    ├── redis/            # Blacklist de JTI (RedisTemplate)
    ├── kafka/            # Publicação de eventos de usuário
    └── web/              # Controladores REST + GlobalExceptionHandler
```

---

## Integração Keycloak

O auth-service não armazena usuários nem senhas localmente. Toda a identidade fica no Keycloak, realm `ecommerce`.

### Registro (`POST /auth/register`)

```
auth-service
  │
  ├─► POST /admin/realms/ecommerce/users          (Admin API — cria usuário)
  ├─► PUT  /admin/realms/ecommerce/users/{id}/reset-password  (define senha)
  ├─► PUT  /admin/realms/ecommerce/users/{id}     (limpa required actions)
  └─► Kafka topic: user → evento CREATED
```

O token de admin é obtido via ROPC no realm `master` com `admin-cli`. A etapa de limpeza de `requiredActions` é necessária em Keycloak 26.x, que adiciona `VERIFY_PROFILE` dinamicamente — ver [nota técnica](#limitações-e-próximos-passos).

### Login (`POST /auth/login`)

Usa o grant type **Resource Owner Password Credentials (ROPC)** no endpoint:

```
POST /realms/ecommerce/protocol/openid-connect/token
grant_type=password
```

Retorna `access_token` (TTL 5 min) e `refresh_token`.

### Refresh (`POST /auth/refresh`)

```
POST /realms/ecommerce/protocol/openid-connect/token
grant_type=refresh_token
```

### Logout (`POST /auth/logout`)

Duas ações em sequência:

1. Revoga o `refresh_token` no Keycloak (`/protocol/openid-connect/logout`)
2. Extrai o `jti` e `exp` do `access_token` (decodificação manual do payload JWT em Base64) e persiste a chave `blacklist:{jti}` no Redis com TTL = tempo restante até `exp`

```
blacklist:{jti}  →  TTL = exp - now()  →  Redis
```

Qualquer serviço que receba o `access_token` pode verificar se o JTI está na blacklist antes de aceitar a requisição.

---

## API Reference

A aplicação sobe na porta `8084`. O Actuator fica na porta `8088`.

### POST /auth/register

Cadastra um novo usuário no Keycloak e publica o evento `user.CREATED` no Kafka.

```bash
curl -s -X POST http://localhost:8084/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "joao",
    "name": "João Silva",
    "password": "Senh@123!"
  }' | jq
```

**`201 Created`**

```json
{
  "data": {
    "keycloakId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "username": "joao"
  }
}
```

---

### POST /auth/login

Autentica o usuário via ROPC e retorna os tokens Keycloak.

```bash
curl -s -X POST http://localhost:8084/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "joao",
    "password": "Senh@123!"
  }' | jq
```

**`200 OK`**

```json
{
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 300
  }
}
```

---

### POST /auth/refresh

Troca o `refreshToken` por um novo par de tokens.

```bash
curl -s -X POST http://localhost:8084/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }' | jq
```

**`200 OK`** — mesmo formato do login.

---

### POST /auth/logout

Revoga o refresh token no Keycloak e insere o JTI do access token na blacklist Redis.

```bash
curl -s -X POST http://localhost:8084/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

**`204 No Content`**

---

### Erros

| Status | Situação |
|---|---|
| `401 Unauthorized` | Credenciais inválidas ou refresh token expirado |
| `409 Conflict` | Username já cadastrado no Keycloak |
| `422 Unprocessable Entity` | Campos inválidos (Bean Validation) |
| `500 Internal Server Error` | Falha ao publicar evento no Kafka |

**Exemplo de erro de validação:**

```json
{
  "errors": [
    { "field": "password", "message": "tamanho deve ser entre 8 e 2147483647" },
    { "field": "username", "message": "não deve estar em branco" }
  ]
}
```

---

## Evento Kafka

Ao concluir o registro com sucesso, o serviço publica no tópico `user`.

| Tópico | Evento | Trigger |
|---|---|---|
| `user` | `CREATED` | `POST /auth/register` bem-sucedido |

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

**Consumidor esperado:** `user-service` — persiste o perfil do usuário no PostgreSQL a partir deste evento.

---

## Como rodar localmente

**Pré-requisitos:** Java 21 e Docker.

### 1. Suba a infraestrutura

Na raiz do repositório:

```bash
docker compose up -d
```

O `docker-compose.yml` inclui automaticamente `docker-compose.data.yml` (MongoDB, Kafka, Elasticsearch, Redis, Schema Registry) e `docker-compose.monitoring.yml` (OTel Collector, Grafana, Loki, Prometheus, Tempo). Para o auth-service os serviços essenciais são Keycloak (`:8089`) e Redis (`:6379`).

### 2. Inicie o serviço

```bash
# A partir de services/auth/
./mvnw spring-boot:run
```

### Variáveis de ambiente (com defaults para dev)

| Variável | Default | Descrição |
|---|---|---|
| `SERVER_PORT` | `8084` | Porta da API |
| `MANAGEMENT_PORT` | `8088` | Porta do Actuator |
| `REDIS_HOST` | `localhost` | Host do Redis |
| `REDIS_PORT` | `6379` | Porta do Redis |
| `KAFKA_BROKERS` | `localhost:9092` | Bootstrap servers do Kafka |
| `KEYCLOAK_SERVER_URL` | `http://localhost:8089` | URL base do Keycloak |
| `KEYCLOAK_REALM` | `ecommerce` | Realm dos usuários |
| `KEYCLOAK_CLIENT_ID` | `auth-service` | Client ID no Keycloak |
| `KEYCLOAK_CLIENT_SECRET` | `auth-service-secret` | Client secret |
| `KEYCLOAK_ADMIN` | `admin` | Usuário admin do realm master |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | Senha do admin |
| `OTEL_ENDPOINT` | `http://localhost:4318` | Endpoint do OTel Collector |

### Actuator

```bash
curl http://localhost:8088/actuator/health
```

```json
{ "status": "UP" }
```

---

## Testes e qualidade

| Sufixo | Executor | Infraestrutura | Descrição |
|---|---|---|---|
| `*Test` | Surefire (`./mvnw test`) | Nenhuma | Testes unitários com Mockito |
| `*IT` | Failsafe (`./mvnw verify`) | Testcontainers | Testes de integração com Keycloak + Redis + Kafka reais |

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

> Classes excluídas do JaCoCo: `AuthApplication`, `*Configuration`, `*Properties`.

Os testes de integração usam **Testcontainers** com containers reais de Keycloak 26.2, Redis 7 e Kafka. Nenhuma infraestrutura local é necessária para rodá-los.

---

## Limitações e próximos passos

### Endpoints sem proteção inbound

Os endpoints `/auth/*` não validam JWT de entrada — qualquer requisição sem token é aceita. A proteção via `@EnableJwtValidation` (da `security-lib` da plataforma) está deferida para quando a lib estiver disponível. **Em produção, proteja os endpoints via API Gateway / Traefik.**

---

## CI

O workflow [`auth-quality.yml`](../../.github/workflows/auth-quality.yml) dispara em todo push e pull request que altere arquivos em `services/auth/`. Executa `./mvnw verify` no runner `ubuntu-latest` e publica o relatório JaCoCo como artefato.
