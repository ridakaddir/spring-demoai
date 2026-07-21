# demoai — watching every token your LLM spends

What does a single LLM request actually *cost* you? Not in the abstract — for *this* request, right now: how many tokens went in, how many came out, and where did they go? Most apps can't answer that. This demo exists to show that with Spring Boot, the answer can be almost free.

**demoai** is a small Spring Boot application that asks a local LLM for developer jokes — and captures the token cost of every single request as a metric, a trace, and a log line, all correlated in Grafana.

## The idea

The app itself is deliberately tiny: one endpoint, `GET /jokes`, backed by a `JokeService` that prompts **Ollama** (running `llama3.2`) through **Spring AI**'s `ChatClient`. The interesting part is everything around it:

- **Spring Modulith** keeps the joke feature a self-contained module — the controller and service are package-private, and `ModularityTests` fails the build if boundaries leak.
- **Spring Boot's Docker Compose support** means there is no setup. Start the app, and it starts Ollama, Postgres, and the whole observability stack for you — it even pulls the `llama3.2` model on first run.
- **Micrometer + OpenTelemetry** carry every signal — metrics, traces, logs — into a local Grafana stack.

## Try it

You need Java 21 and Docker. Nothing else.

```bash
./mvnw spring-boot:run
```

The first startup takes a few minutes (containers plus the model pull). Then:

```bash
curl localhost:8080/jokes
# {"joke":"Why do programmers prefer dark mode? Because light attracts bugs."}
```

Now open [Grafana at localhost:3000](http://localhost:3000) (no login needed) and look at the **demoai observability** dashboard. Every joke you just requested is there: token rate, tokens per request, cumulative usage.

## How the token capture works

Here's the surprising part: **Spring AI already does most of the work.** Every `ChatClient` call runs inside a Micrometer observation, which auto-emits:

- a `gen_ai_client_token_usage_total` counter (input/output/total tokens, tagged with the model), and
- a `chat llama3.2` span carrying `gen_ai.usage.input_tokens`, `gen_ai.usage.output_tokens`, and `gen_ai.usage.total_tokens`.

So metrics and traces come for free the moment the observability plumbing exists. What Spring AI *doesn't* give you is the per-request view in your own code — and that's a one-line trap. The fluent API's `.call().content()` returns just the text and throws the metadata away. Switch to `.call().chatResponse()` and the usage is right there:

```java
ChatResponse response = chatClient.prompt()
        .user("Tell me a joke about software developers.")
        .call()
        .chatResponse();          // not .content() — that discards the metadata!

Usage usage = response.getMetadata().getUsage();
```

`JokeService` uses that to do two things the framework can't:

1. **Log the cost per request**, inside the request's trace context:
   ```
   LLM returned a joke (65 chars) — tokens: prompt=61 completion=13 total=74
   ```
   Because the OpenTelemetry Logback appender stamps `trace_id`/`span_id` onto every log record, this line is clickable in Grafana — from the log in Loki straight to the trace in Tempo.

2. **Record a `llm.tokens.per.request` distribution summary** with p50/p95 percentiles. The built-in counter can only give you rates and averages; the summary tells you what a *typical* request costs versus an expensive outlier.

One request, three correlated views:

| Surface | What you see |
|---|---|
| **Prometheus** | `gen_ai_client_token_usage_total` + `llm_tokens_per_request` p50/p95 |
| **Tempo** | the chat span with exact token counts as attributes |
| **Loki** | the per-request log line, linked to its trace |

## The observability wiring

One design decision worth calling out: the three signals travel different roads, on purpose.

- **Traces** are pushed over OTLP/HTTP to Tempo (`localhost:4318`).
- **Logs** are pushed over OTLP/HTTP to Loki (`localhost:3100`).
- **Metrics** are *pulled* — Prometheus scrapes `/actuator/prometheus`. The OTLP metrics registry is explicitly excluded from the build, because Tempo's OTLP receiver only accepts traces anyway and the scrape model is the Prometheus-native way.

Grafana is fully provisioned from [`monitoring/`](monitoring/) — datasources (including the Loki→Tempo derived-field link on `trace_id`) and the dashboard land automatically; there's nothing to click together.

Everything runs from one `compose.yaml`:

| Service | Port(s) | Profile |
|---|---|---|
| postgres (`mydatabase` / `myuser`) | 5432 | *always on* |
| ollama | 11434 | `full` |
| tempo | 4318 (OTLP), 3200 (query) | `full` |
| prometheus | 9090 | `full` |
| loki | 3100 | `full` |
| grafana | 3000 | `full` |

The app runs with the `full` compose profile active, so a plain `spring-boot:run` brings the whole stack up.

## The supporting cast

**Database & migrations.** Postgres is migrated by Flyway (`src/main/resources/db/migration/`). Migrations run on normal startup, but there's also a dedicated one-shot mode — headless, Postgres-only, exits when done (`MigrateRunner`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=migrate
```

**Code layout.** Small enough to read in one sitting:

```
src/main/java/com/ridakaddir/demoai/
├── DemoaiApplication.java            # entry point
├── MigrateRunner.java                # one-shot Flyway mode (profile: migrate)
├── OpenTelemetryAppenderConfig.java  # wires Logback → OTLP log export
└── joke/                             # Spring Modulith module
    ├── JokeController.java           # GET /jokes (package-private)
    └── JokeService.java              # ChatClient call + token metrics
```

**Tests.** `./mvnw test` — including `ModularityTests`, which verifies the Modulith module boundaries.

## Takeaway

If your stack already speaks Micrometer and OpenTelemetry, per-request LLM cost tracking is not a project — it's an afternoon. Spring AI emits the token telemetry on its own; your job is just to not throw the metadata away, and to give Grafana something nice to draw. And since it's all token-denominated, the day you swap the local Ollama for a paid API, multiplying those same streams by a price per token is all that's left to do.

---

*Stack: Java 21 · Spring Boot 4.1.0 · Spring AI 2.0.0 · Spring Modulith 2.1.0 · Ollama (`llama3.2`) · PostgreSQL + Flyway · Grafana / Prometheus / Tempo / Loki*
