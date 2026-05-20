# SEDNA Operator Guide (Phase 14)

Local and CI workflows for semantic DNA pipelines. Example project layout: [examples/README.md](../examples/README.md).

## Prerequisites

- Java 21
- `./gradlew build` green
- Optional: PostgreSQL (Docker Compose in repo root) for JDBC checkpoints
- Optional: Graphviz (`dot`) to render `.dot` files

## Local workflow (cms-reference)

```bash
# 1. Reverse project → DNA
./gradlew :sedna-cli:run --args="reverse --input=examples/sedna-cms/cms-reference --output=/tmp/cms.sdna"

# 2. Validate DNA
./gradlew :sedna-cli:run --args="validate --input=/tmp/cms.sdna"

# 3. Visualize semantic graph
./gradlew :sedna-cli:run --args="visualize --input=/tmp/cms.sdna --output=/tmp/cms.dot"
dot -Tpng /tmp/cms.dot -o /tmp/cms.png

# 4. Forward → Spring Boot tree
./gradlew :sedna-cli:run --args="forward --input=/tmp/cms.sdna --output=/tmp/generated"

# 5. Runtime + file checkpoints
./gradlew :sedna-cli:run --args="run --input=/tmp/cms.sdna --checkpoint-dir=/tmp/checkpoints --monitor-port=8080"

# 6. Replay from checkpoint store
./gradlew :sedna-cli:run --args="replay --checkpoint-dir=/tmp/checkpoints --format=json"
```

Installable distribution: `./gradlew :sedna-cli:installDist` → `sedna-cli/build/install/sedna-cli/bin/sedna-cli`.

## CI workflow (JSON output)

All commands support `--format=json` for machine-readable status:

```bash
sedna validate --input=examples/sedna-e2e-tests/cms-reference-fixture.sdna --format=json
sedna diff --left=baseline.sdna --right=candidate.sdna --format=json
sedna run --input=fixture.sdna --checkpoint-dir=checkpoints --format=json
sedna replay --checkpoint-dir=checkpoints --format=json
```

Exit codes:

| Code | Meaning |
|------|---------|
| 0 | Success (diff: graphs equivalent) |
| 1 | Semantic or I/O error (diff: graphs differ) |
| 2 | Missing required flags |

## Checkpoint stores

| Flag | Use case |
|------|----------|
| `--checkpoint-dir=<path>` | Local replay without database |
| `--checkpoint-jdbc-url=<jdbc>` | PostgreSQL persistence (see `docker-compose.yml`) |

`sedna replay` requires a persistent store (`--checkpoint-dir` or JDBC).

## Training corpus

```bash
sedna train --corpus=. --output=training-out --format=json
# artifacts: training-out/dataset.manifest, dataset.manifest.sha256, reproducibility.report
```

## Monitoring endpoint

```bash
sedna monitor --input=examples/sedna-e2e-tests/cms-reference-fixture.sdna --port=8080
curl http://127.0.0.1:8080/health
curl http://127.0.0.1:8080/trace
```

Or combine with run: `sedna run --input=... --monitor-port=8080`.

## Current version limitations

The operator guide covers **local and CI** workflows only. The following are **not supported in the current release** and will be addressed only after core functionality (Java/Spring DNA pipelines, runtime, mutation, training) is complete:

- Multi-language projects (Kotlin, TypeScript, etc.)
- Distributed runtime, Kafka, Kubernetes / cloud deployment
- Cluster-wide orchestration or cross-service production semantics

Use local execution, file or JDBC checkpoints, and the monitoring endpoint on a single node.

## IntelliJ / IDE

Use CLI `visualize` + Graphviz for DNA inspection. A dedicated IntelliJ plugin is deferred; see `tooling/intellij-sedna/README.md`.

## Equivalence gates

Run full suite:

```bash
./gradlew build
```

Key integration tests: `Phase14AcceptanceTest`, `SpringBootReverseForwardEquivalenceTest`, `RuntimeReplayTest`.
