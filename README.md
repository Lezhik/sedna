# SEDNA

**Semantic DNA Runtime & Transformation System**

SEDNA is a deterministic semantic execution platform that converts software systems into compact semantic DNA representations and reconstructs executable applications from those representations.

The project combines:

* semantic graph modeling
* deterministic runtime execution
* bidirectional code ↔ DNA transformation
* semantic mutation and folding
* AI-assisted semantic enrichment
* replayable execution semantics

---

# Status

| Component           | Status      |
| ------------------- | ----------- |
| Specification Suite | v1 Complete |
| Implementation      | MVP Phase 6 (training pipeline + CLI) |
| Runtime Profiles    | DAG Ready   |
| STATEFUL Runtime    | Planned     |
| SUPERVISOR Runtime  | Planned     |
| Training Pipeline   | Planned     |

Specification baseline: **2026-Q2**

---

# Core Principles

* Deterministic execution is mandatory
* DNA serialization must be stable
* Semantic transformations must be reproducible
* Runtime ordering must be canonical
* LLMs may enrich semantics but never define structure
* Non-deterministic outputs are forbidden inside final DNA

---

# High-Level Architecture

```text id="a3g8rf"
                           +----------------------+
                           |     sedna-core       |
                           | DTOs + Contracts     |
                           | Canonical Ordering   |
                           +----------+-----------+
                                      |
        ----------------------------------------------------------------
        |              |               |               |                |
        v              v               v               v                v

+---------------+  +---------------+  +---------------+  +---------------+
| sedna-reverse |  | sedna-forward |  | sedna-runtime |  | sedna-training|
| Code -> DNA   |  | DNA -> Code   |  | DNA Execution |  | Dataset Build |
+-------+-------+  +-------+-------+  +-------+-------+  +-------+-------+
        |                  |                  |                  |
        ----------------------------------------------------------
                                  |
                                  v
                      +-----------------------+
                      |      sedna-dna        |
                      | Binary DNA Storage    |
                      +-----------+-----------+
                                  |
                 -------------------------------------------
                 |                                         |
                 v                                         v
       +----------------------+               +----------------------+
       |   sedna-mutation     |               |  sedna-validation    |
       | Semantic Deltas      |               | Determinism Rules    |
       +----------------------+               +----------------------+
                 |                                         |
                 -------------------------------------------
                                  |
                                  v

                      +-----------------------+
                      |   Semantic Registry   |
                      | Vocabulary + Motifs   |
                      +-----------------------+
```

**Note:** all modules depend on `sedna-core` for canonical DTOs, contracts, comparators, deterministic ordering, and shared utilities.

---

# Main Components

| Module              | Responsibility                                |
| ------------------- | --------------------------------------------- |
| `sedna-core`        | Canonical DTOs, contracts, ordering utilities |
| `sedna-dna`         | Binary DNA serialization and decoding         |
| `sedna-reverse`     | Existing code → semantic DNA                  |
| `sedna-forward`     | Semantic DNA → generated project              |
| `sedna-runtime`     | Runtime execution engine                      |
| `sedna-training`    | Dataset and mutation learning                 |
| `sedna-mutation`    | Semantic graph mutation engine                |
| `sedna-validation`  | Determinism and semantic validation           |
| `sedna-registry`    | Vocabulary, motifs, contracts                 |
| `sedna-persistence` | Runtime persistence and checkpoints           |

---

# Runtime Profiles

| Profile    | Determinism        | Loops          | State        | Replay       |
| ---------- | ------------------ | -------------- | ------------ | ------------ |
| DAG        | Strict             | Forbidden      | Stateless    | Full         |
| STATEFUL   | Strict             | Controlled FSM | Persistent   | Checkpoint   |
| SUPERVISOR | Semi-deterministic | Allowed        | Compensating | Event Replay |

---

# Repository Structure

```text id="xv9az1"
sedna/
├── sedna-core/
├── sedna-dna/
├── sedna-registry/
├── sedna-forward/
├── sedna-reverse/
├── sedna-runtime/
├── sedna-training/
├── sedna-mutation/
├── sedna-validation/
├── sedna-persistence/
├── examples/
├── benchmarks/
├── tests/
└── docs/
```

---

# Technology Stack

| Area                       | Technology            |
| -------------------------- | --------------------- |
| Language                   | Java 21               |
| Build System               | Gradle                |
| AST Analysis (primary)     | Spoon                 |
| AST Analysis (lightweight) | JavaParser            |
| Bytecode Analysis          | ASM                   |
| Graph Processing           | JGraphT               |
| Runtime FSM                | Spring State Machine  |
| Async Execution            | Project Reactor       |
| Binary Encoding            | Custom TLV            |
| Persistence                | PostgreSQL            |
| Embeddings                 | Deterministic encoder |
| LLM Integration            | OpenRouter (HTTP API) |

---

# Determinism Rules

Forbidden:

* unordered `HashMap` iteration
* random UUID generation
* system-time-based ordering
* non-canonical graph traversal
* unstable parallel execution
* implicit runtime reflection ordering

Required:

* canonical node ordering
* stable SHA-256 hashing
* deterministic topological sorting
* explicit comparator utilities
* stable replay semantics

Allowed collections:

```text id="s5o8i9"
LinkedHashMap
TreeMap
ImmutableList
ImmutableMap
```

Canonical comparator source:

```text id="g3m2ld"
sedna-core::CanonicalOrdering
```

---

# Getting Started

## Build

```bash id="t2x7kq"
./gradlew build
```

## Run Tests

```bash id="k6q9fw"
./gradlew test
```

## Run Benchmarks

```bash id="e0n2ac"
./gradlew jmh
```

---

# Minimal Workflow

## Reverse Pipeline

Existing Spring Boot project → semantic DNA

```bash id="u5j1yr"
./gradlew :sedna-reverse:run \
  --args="--input=examples/cms-reference"
```

Expected output:

```text id="d0v8qe"
examples/cms-reference.sdna
```

---

## Forward Pipeline

Semantic DNA → generated Spring Boot project

```bash id="w8m1zn"
./gradlew :sedna-forward:run \
  --args="--input=examples/cms-reference.sdna --output=./generated"
```

Expected output:

```text id="r4f3kc"
generated/
├── src/
├── build.gradle
└── application.yml
```

---

# Determinism Validation

Repeated executions must produce identical results.

Validation examples:

```text id="n6d0qs"
encode(graph) == encode(graph)
decode(encode(graph)) == graph
forward(reverse(project)) preserves semantic equivalence
```

Canonical topological ordering:

```text id="h2z7lp"
dependency topology
→ canonical node ID tiebreaker
```

---

# Training Dataset Scope

Primary MVP dataset:

```text id="v1s7xy"
Spring Boot REST CMS applications
```

Minimum dataset:

```text id="q7l3vr"
20-30 projects
```

Recommended dataset:

```text id="m9u2co"
100-300 projects
```

Repository policy:

```text id="c8k4ez"
Git history analyzed per project directory,
not per repository globally.
```

Cross-project semantic reconstruction is forbidden.

---

# AI-Agent Development Model

The project is designed for multi-agent implementation.

Rules:

* Contract-first development
* Shared DTOs only from `sedna-core`
* No duplicated model definitions
* No raw exceptions across module boundaries
* Additive interface evolution only
* All public APIs deterministic

LLM usage:

| Allowed                     | Forbidden                 |
| --------------------------- | ------------------------- |
| semantic enrichment         | graph topology generation |
| UNKNOWN node classification | NodeID generation         |
| method body suggestions     | contract mutation         |
| documentation assistance    | runtime scheduling        |

MVP sandbox model:

```text id="p4x1ba"
Separate process via HTTP API only.
No in-process LLM execution.
Provider: OpenRouter (https://openrouter.ai/api/v1).
```

Configuration (environment):

```text
SEDNA_LLM_ENABLED=false          # default; CI uses false
SEDNA_LLM_BASE_URL=https://openrouter.ai/api/v1
SEDNA_LLM_MODEL=openai/gpt-4o-mini
SEDNA_LLM_TIMEOUT_MS=30000       # HTTP read timeout; no retry in MVP
OPENROUTER_API_KEY=<secret>      # required when LLM enabled
```

---

# MVP Scope

Included:

* DAG runtime profile
* deterministic DNA serialization
* Spring Boot REST reconstruction
* semantic registry
* reverse pipeline
* forward pipeline
* mutation engine
* replay validation

Excluded:

* distributed orchestration
* Kafka integration
* Kubernetes deployment
* multi-language support
* cross-service transactions
* IntelliJ plugin
* visualization dashboard

Post-MVP tooling:

* IntelliJ plugin
* semantic graph visualization
* live runtime monitoring

---

# Performance Targets

Baseline environment:

```text id="f8w6dc"
4-core CPU
16GB RAM
SSD storage
Warm JVM
```

Reference project:

```text id="y1n4op"
examples/cms-reference
```

Targets:

| Operation              | Target |
| ---------------------- | ------ |
| DNA decode             | <100ms |
| DNA encode             | <100ms |
| Reverse analysis       | <30s   |
| Forward reconstruction | <5s    |
| Runtime graph startup  | <3s    |

---

# Documentation

Core specifications:

```text id="b7x0er"
/docs/sedna_formal_semantic_specification_v_01.md
/docs/sedna_forward_pipeline_specification_v_01.md
/docs/sedna_reverse_pipeline_specification_v_01.md
/docs/sedna_training_pipeline_specification_v_01.md
/docs/sedna_execution_semantics_runtime_model_v_01.md
```

---

# Development Workflow

```text id="u6m9af"
1. Define contracts
2. Define DTOs in sedna-core
3. Implement deterministic tests
4. Implement module logic
5. Validate replay consistency
6. Run semantic equivalence tests
```

---

# License

Apache License 2.0 — see [LICENSE](LICENSE).

---

# Final Principle

SEDNA stores executable semantics — not source code.
