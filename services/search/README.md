# java-base-project

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?logo=springboot)
![Quality](https://github.com/Vagnerlg/java-spring-boot-base-project/actions/workflows/quality.yml/badge.svg)
![Quality](https://github.com/Vagnerlg/java-spring-boot-base-project/actions/workflows/quality.yml/badge.svg)
![Tests](https://img.shields.io/badge/tests-JUnit%205%20%2B%20Testcontainers-blue?logo=junit5)

Projeto base para novas aplicações **Spring Boot 4 + Java 21**. Fornece a estrutura e os componentes transversais prontos para uso, para que cada novo serviço comece com qualidade e observabilidade já configuradas.

| Componente | Tecnologia | Finalidade |
|---|---|---|
| Actuator | Spring Boot Actuator | Health check e métricas expostas |
| Observabilidade | OpenTelemetry + Micrometer | Traces, métricas e logs via OTLP |
| Logs estruturados | Logback + OTel Appender | Logs correlacionados com traces |
| Testes | JUnit 5 + Mockito + Testcontainers | Unitários e integração |
| Qualidade | JaCoCo + SpotBugs + PMD | Cobertura e análise estática |
| CI | GitHub Actions | Build e quality gates automáticos |

---

## Índice

- [Rodando em desenvolvimento](#rodando-em-desenvolvimento)
- [Observabilidade](#observabilidade)
- [Logs](#logs)
- [Testes e qualidade de código](#testes-e-qualidade-de-código)
- [Como escrever testes](#como-escrever-testes)
  - [Teste unitário puro](#teste-unitário-puro)
  - [Teste unitário com mock](#teste-unitário-com-mock-mockito-puro)
  - [Teste com mock de bean Spring](#teste-com-mock-de-bean-spring-mockitobean)
  - [Teste de integração com Testcontainers](#teste-de-integração-com-testcontainers)
- [CI — GitHub Actions](#ci--github-actions)

---

## Rodando em desenvolvimento

### Pré-requisitos

- Java 21
- Docker Desktop — para a infra de observabilidade e testes de integração

### 1. Suba a infraestrutura local

```bash
docker compose up -d
```

Inicia: OTel Collector, Grafana Tempo, Loki, Prometheus e Grafana.

### 2. Inicie a aplicação

```bash
# Logs em texto legível (padrão para dev)
./mvnw spring-boot:run

# Logs em JSON estruturado (ECS format)
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

Actuator disponível em `http://localhost:8081`:

| Endpoint | Descrição |
|---|---|
| `GET /actuator/health` | Status da aplicação e subsistemas |
| `GET /actuator/info` | Metadados da aplicação |
| `GET /actuator/metrics` | Lista todas as métricas disponíveis |

---

## Observabilidade

A aplicação envia os três sinais via **OTLP HTTP** para um OTel Collector centralizado:

```
App (:8081)
  │
  └─ OTLP HTTP (:4318) ──► OTel Collector
                                 ├─ Traces   ──► Grafana Tempo  (:3200)
                                 ├─ Métricas ──► Prometheus     (scrape :8889)
                                 └─ Logs     ──► Loki           (:3100)
                                                      │
                                                Grafana (:3000)
```

### Acessos locais

| Serviço | URL | Credenciais |
|---|---|---|
| Grafana | http://localhost:3000 | `admin` / `admin` |
| Prometheus | http://localhost:9090 | — |

### Sinais exportados

| Sinal | O que é capturado |
|---|---|
| **Traces** | Spans gerados automaticamente pelo Spring para cada operação instrumentada |
| **Métricas** | JVM, pool de threads e métricas customizadas via Micrometer |
| **Logs** | Todos os logs do Logback com `traceId` e `spanId` no contexto |

### Configuração

```yaml
# application.yaml
management:
  tracing:
    sampling:
      probability: 1.0          # 100% em dev — reduza em produção (ex: 0.1)
  otlp:
    metrics:
      export:
        url: http://localhost:4318/v1/metrics
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/traces
    logging:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/logs
  logging:
    export:
      otlp:
        enabled: true
```

> [!IMPORTANT]
> `management.logging.export.otlp.enabled: true` é obrigatório no Spring Boot 4. Sem essa propriedade o bean `OtlpHttpLogRecordExporter` não é criado e os logs são descartados silenciosamente.

---

## Logs

Os logs são exportados via `OpenTelemetryAppender` (`logback-spring.xml`). Cada linha carrega automaticamente o `traceId` e `spanId` do contexto em curso, permitindo navegar de um trace no Grafana Tempo diretamente para os logs no Loki.

Em desenvolvimento os logs são impressos em texto plano. Com o profile `prod` o formato muda para **JSON estruturado (ECS)**:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar app.jar
```

### Adicionando logs

```java
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    public PedidoResponse buscar(Long id) {
        log.info("Buscando pedido id={}", id);
        // ...
    }
}
```

### Enriquecendo com MDC

Campos adicionados ao MDC aparecem como atributos no Loki e ficam disponíveis como filtros no Grafana:

```java
MDC.put("pedido.id", id.toString());
MDC.put("pedido.status", status);
try {
    log.info("Processando pagamento");
} finally {
    MDC.remove("pedido.id");
    MDC.remove("pedido.status");
}
```

### Exemplo futuro: JDBC tracing

<details>
<summary>Como adicionar spans de queries SQL ao trace</summary>

Ao integrar um banco de dados, adicione `datasource-micrometer-spring-boot` para obter spans automáticos de cada query SQL como filhos do trace:

```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer-spring-boot</artifactId>
    <version>2.2.1</version>
</dependency>
```

Nenhuma configuração extra é necessária. O Spring Boot auto-configura um proxy na `DataSource` que instrumenta qualquer tecnologia JDBC: JPA, Spring Data JDBC, JdbcTemplate, jOOQ, Flyway, etc.

O trace no Grafana Tempo ganha a estrutura completa:

```
HTTP POST /pedidos                          (200ms)
  └─ PedidoService.criar                   (180ms)
       ├─ SELECT * FROM produtos WHERE ...  (12ms)
       └─ INSERT INTO pedidos ...           (8ms)
```

</details>

---

## Testes e qualidade de código

### Nomenclatura e executores

| Sufixo | Executor | Contexto Spring | Docker |
|---|---|---|---|
| `*Test` | Surefire — `./mvnw test` | Não | Não |
| `*IT` | Failsafe — `./mvnw verify` | Sim (`@SpringBootTest`) | Sim |

O Surefire executa apenas `*Test` — rápido, sem infraestrutura. O Failsafe executa `*IT` após o empacotamento e garante teardown mesmo em caso de falha.

### Comandos

```bash
# Apenas testes unitários (sem Docker)
./mvnw test

# Build completo: unitários + integração + cobertura + análise estática
./mvnw verify

# Relatório de cobertura sem forçar threshold
./mvnw test jacoco:report
# → target/site/jacoco/index.html

# Apenas análise estática
./mvnw spotbugs:check pmd:check
```

### Quality gates

Todos executados automaticamente em `./mvnw verify`:

| Ferramenta | O que verifica | Falha o build se... |
|---|---|---|
| **JaCoCo** | Cobertura de testes | LINE ou BRANCH < 80% |
| **SpotBugs** | Bugs em bytecode | Qualquer bug encontrado |
| **PMD** | Qualidade do código-fonte | Qualquer violação |

> [!NOTE]
> Classes excluídas do JaCoCo: `*Application`, `*Configuration`, `*Properties`.

Para suprimir um falso positivo pontual:

```java
// SpotBugs
@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH", justification = "valor nunca é null por invariante do domínio")

// PMD
@SuppressWarnings("PMD.NomeDaRegra")
```

---

## Como escrever testes

### Teste unitário puro

Use quando a classe não tem dependências externas. Não sobe contexto Spring.

```java
class PedidoCalculadorTest {

    private final PedidoCalculador calculador = new PedidoCalculador();

    @Test
    void deveCalcularTotalComDesconto() {
        BigDecimal total = calculador.calcular(new BigDecimal("100.00"), 10);

        assertThat(total).isEqualByComparingTo("90.00");
    }
}
```

### Teste unitário com mock (Mockito puro)

Use quando a classe tem dependências que precisam ser isoladas. Sem Spring, sem Docker — o mais rápido.

```java
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository repository;

    @InjectMocks
    private PedidoService service;

    @Test
    void deveBuscarPedidoPorId() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Pedido(1L, "Em andamento")));

        var resultado = service.buscar(1L);

        assertThat(resultado.status()).isEqualTo("Em andamento");
        verify(repository).findById(1L);
    }

    @Test
    void deveLancarExcecaoQuandoNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOf(PedidoNaoEncontradoException.class);
    }
}
```

### Teste com mock de bean Spring (`@MockitoBean`)

Use quando precisa do contexto Spring mas quer substituir um bean por um mock — útil para isolar dependências externas como clientes HTTP ou gateways.

Declare os mocks em `MockConfiguration` (já presente em `src/test`):

```java
@TestConfiguration(proxyBeanMethods = false)
public class MockConfiguration {

    @Bean
    PagamentoGateway pagamentoGateway() {
        return Mockito.mock(PagamentoGateway.class);
    }
}
```

```java
@SpringBootTest
@Import(MockConfiguration.class)
class PedidoServiceSpringTest {

    @Autowired
    private PedidoService service;

    @MockitoBean
    private PedidoRepository repository;

    @Test
    void deveBuscarPedidoComContextoSpring() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Pedido(1L, "Aprovado")));

        assertThat(service.buscar(1L).status()).isEqualTo("Aprovado");
    }
}
```

### Teste de integração com Testcontainers

Use para testar o fluxo completo com infraestrutura real em Docker. O sufixo `IT` garante execução no Failsafe, onde o Docker está disponível.

**Passo 1 — Configure o container em `TestcontainersConfiguration`** (já presente em `src/test`):

```java
@TestConfiguration(proxyBeanMethods = false)
@Testcontainers
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"));
    }
}
```

> [!TIP]
> `@ServiceConnection` configura automaticamente `spring.datasource.*` apontando para o container — sem properties manuais. Adicione `org.testcontainers:postgresql` no `pom.xml` ao usar PostgreSQL.

**Passo 2 — Escreva o teste:**

```java
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PedidoRepositoryIT {

    @Autowired
    private PedidoRepository repository;

    @Test
    @Transactional
    void devePersistirEBuscarPedido() {
        var pedido = repository.save(new Pedido("Novo"));

        var encontrado = repository.findById(pedido.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getStatus()).isEqualTo("Novo");
    }
}
```

---

## CI — GitHub Actions

O workflow [`.github/workflows/quality.yml`](.github/workflows/quality.yml) roda automaticamente em todo push e pull request.

```
push / pull_request
       │
       ▼
  Checkout + Java 21 (Temurin, cache Maven)
       │
       ▼
  ./mvnw verify
  ├── Compila
  ├── Testes unitários (Surefire)
  ├── Testes de integração (Failsafe + Docker)
  ├── Cobertura JaCoCo ≥ 80%
  ├── SpotBugs
  └── PMD
       │
       ▼
  Upload relatório JaCoCo (artefato, 1 dia)
```

> [!NOTE]
> O runner `ubuntu-latest` já possui Docker instalado e em execução — por isso os testes de integração com Testcontainers funcionam no CI sem configuração adicional.
