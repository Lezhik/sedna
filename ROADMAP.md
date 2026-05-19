# SEDNA Project Roadmap

Version: v1.0  
Status: Active  
Last Updated: 2026-Q2

---

# 1. Roadmap Principles

The SEDNA roadmap follows five core engineering principles:

1. Determinism before performance
2. Canonical semantic structures before optimization
3. Validation before execution
4. Local-only execution before distributed scaling
5. Stable interfaces before feature expansion

The roadmap is intentionally incremental.
Every phase must produce a stable and testable system state.

---

# 2. MVP Scope

## 2.1 Included in MVP

- Canonical DNA binary format
- Semantic graph model
- Registry system
- Forward pipeline
- Reverse pipeline
- DAG runtime profile
- Mutation engine
- Validation engine
- Local execution
- Deterministic replay
- Spring Boot project support

---

## 2.2 Excluded from MVP

The following capabilities are explicitly postponed:

- Distributed execution
- Kubernetes integration
- Kafka orchestration
- Cloud-native deployment
- Multi-language runtime
- Self-modifying runtime execution
- Autonomous semantic evolution
- Cross-project semantic linking
- UI dashboards
- IntelliJ plugin
- Remote graph databases

---

# 3. Estimated Timeline

| Phase | Name | Duration |
|---|---|---|
| Phase 0 | Foundation | 2 weeks |
| Phase 1 | DNA Core | 4 weeks |
| Phase 2 | Forward Pipeline | 5 weeks |
| Phase 3 | Reverse Pipeline | 5 weeks |
| Phase 4 | Runtime Engine | 4 weeks |
| Phase 5 | Mutation + Advanced Validation | 4 weeks |
| Phase 6 | Training Pipeline | 4 weeks |
| Phase 7 | Stabilization | 2 weeks |

Total sequential estimate:
30 weeks

Parallel AI-agent execution estimate:
24-26 weeks

---

# 4. Global Engineering Constraints

The following constraints apply to all phases.

## 4.1 Determinism

The system must produce identical outputs for identical inputs.

Forbidden:

- unordered iteration
- unstable UUID generation
- random graph traversal
- JVM-dependent ordering
- scheduler-dependent ordering

Allowed:

- canonical node ordering
- deterministic hashing
- lexicographic comparators
- stable serialization

---

## 4.2 Local Execution

The full platform must execute locally.

Forbidden:

- cloud-only dependencies
- remote graph databases
- mandatory internet connectivity
- external orchestration requirements

---

## 4.3 Interface Stability

Public interfaces stabilize after Phase 2.

Allowed after stabilization:

- additive API extensions
- optional parameters
- new interfaces

Forbidden after stabilization:

- breaking public API changes
- DTO schema replacement
- semantic contract rewrites

---

# 5. Phase 0 — Foundation

Duration: 2 weeks

## Goals

Establish canonical project structure and shared semantic contracts.

---

## Deliverables

- Gradle multi-module setup
- CI pipeline
- Canonical DTOs
- Semantic error model
- Deterministic ordering utilities
- Registry bootstrap
- Validation framework skeleton

---

## Required Modules

- sedna-core
- sedna-registry
- sedna-validation

Note:
`sedna-cli` is intentionally postponed until executable pipelines exist.

---

## Algorithms and Technologies

| Area | Technology |
|---|---|
| Language | Java 21 |
| Build system | Gradle |
| Testing | JUnit 5 |
| Static analysis | SpotBugs |
| Formatting | Spotless |
| Serialization | Custom binary TLV (SEDNA-BIN-v1) |

---

## Acceptance Criteria

- project builds successfully
- deterministic ordering utilities validated
- registry bootstrap operational
- CI pipeline passes
- DTO contracts frozen

---

# 6. Phase 1 — DNA Core

Duration: 4 weeks

## Goals

Implement canonical DNA encoding and decoding.

---

## Deliverables

- DNA encoder
- DNA decoder
- motif folding
- semantic hashing
- registry references
- binary persistence

---

## Algorithms

| Component | Algorithm |
|---|---|
| Node hashing | SHA-256 |
| Compression | motif folding |
| Encoding | canonical TLV |
| Dependency ordering | topological sorting |
| Equality | semantic equivalence |

---

## Acceptance Criteria

- encode(decode(x)) == x
- deterministic binary output
- registry references resolve correctly
- motif expansion deterministic
- no unstable serialization

---

# 7. Phase 2 — Forward Pipeline

Duration: 5 weeks

## Goals

Generate deterministic Spring Boot projects from DNA.

---

## Deliverables

- semantic graph expansion
- template engine
- code generation
- dependency generation
- Spring Boot project reconstruction
- initial sedna-cli support

---

## Technologies

| Area | Technology |
|---|---|
| Template engine | JavaPoet |
| Build generation | Gradle |
| Spring generation | Spring Boot 3 |
| Validation | sedna-validation |

---

## Acceptance Criteria

- generated project compiles
- deterministic code generation
- stable package ordering
- identical output across runs
- validation passes before generation

---

# 8. Phase 3 — Reverse Pipeline

Duration: 5 weeks

## Goals

Extract semantic graphs from existing projects.

---

## Parallel Development Rule

Phase 3 and Phase 4 may execute in parallel after:

- sedna-core complete
- sedna-registry complete
- sedna-validation complete
- SemanticGraph DTO stabilized
- contract resolution operational

Full forward pipeline completion is NOT required.

---

## Deliverables

- AST analysis
- semantic extraction
- graph reconstruction
- delta extraction
- trajectory generation

---

## Technologies

| Area | Technology |
|---|---|
| AST analysis (primary) | Spoon |
| AST analysis (lightweight) | JavaParser |
| Bytecode analysis | ASM |
| Git traversal | JGit |
| Graph construction | custom semantic graph |

---

## Acceptance Criteria

- semantic extraction deterministic
- identical NodeID generation
- semantic equivalence preserved
- reverse(forward(x)) stable
- no cross-project semantic linking

---

# 9. Phase 4 — Runtime Engine

Duration: 4 weeks

## Goals

Execute semantic graphs deterministically.

---

## MVP Runtime Scope

Included:

- DAG profile

Deferred:

- STATEFUL profile
- SUPERVISOR profile

---

## Deliverables

- DAG scheduler
- execution engine
- deterministic replay
- checkpointing
- runtime validation

---

## Technologies

| Area | Technology |
|---|---|
| Async execution | Project Reactor |
| Persistence | PostgreSQL |
| FSM runtime | Spring State Machine |
| Replay storage | binary snapshots |

---

## Acceptance Criteria

- deterministic replay works
- canonical execution ordering preserved
- runtime validation operational
- checkpoint recovery works
- replay outputs identical

---

# 10. Phase 5 — Mutation Engine + Advanced Validation

Duration: 4 weeks

## Important Note

Core validation engine is initialized in Phase 0.

Phase 5 extends validation with:

- mutation validation
- semantic equivalence verification
- rollback validation
- mutation safety constraints

---

## Goals

Implement controlled semantic mutation and advanced validation.

---

## Deliverables

- subtree mutation
- rollback engine
- mutation scoring
- semantic equivalence validation
- mutation safety validation

---

## Technologies

| Area | Technology |
|---|---|
| Mutation engine | custom semantic rewrite engine |
| Similarity scoring | deterministic embeddings |
| Validation | graph constraint validation |
| Rollback | snapshot restoration |

---

## Acceptance Criteria

- subtree replacement deterministic
- rollback restores valid state
- mutation validation stable
- semantic equivalence verified
- invalid mutations rejected

---

# 11. Phase 6 — Training Pipeline

Duration: 4 weeks

## Goals

Train semantic registries and mutation datasets.

---

## Deliverables

- project ingestion
- semantic trajectory extraction
- motif discovery
- embedding generation
- registry updates

---

## Technologies

| Area | Technology |
|---|---|
| Git traversal | JGit |
| Embeddings | deterministic semantic encoder |
| Vector indexing | Pure Java approximate NN (post-MVP: FAISS optional) |
| Batch processing | Custom deterministic pipeline runner |

---

## Acceptance Criteria

- training deterministic
- registry updates reproducible
- motif extraction stable
- embeddings reproducible
- mutation dataset generated

---

# 12. Phase 7 — Stabilization

Duration: 2 weeks

## Goals

Harden the platform for production readiness.

---

## Deliverables

- benchmark suite
- fuzz testing
- deterministic stress testing
- replay verification
- performance optimization
- CLI stabilization

---

## Acceptance Criteria

- all benchmarks pass
- deterministic replay validated
- mutation fuzz tests stable
- no unresolved race conditions
- all public APIs documented

---

# 13. Release Plan

| Version | Scope | Validation Target |
|---|---|---|
| v0.1 | DNA core | Phase 1 acceptance criteria |
| v0.2 | Forward pipeline | Phase 2 acceptance criteria |
| v0.3 | Reverse pipeline | Phase 3 acceptance criteria |
| v0.4 | Runtime DAG | Phase 4 acceptance criteria |
| v0.5 | Mutation engine | Phase 5 acceptance criteria |
| v0.6 | Training pipeline | Phase 6 acceptance criteria |
| v1.0 | Stabilized MVP | Phase 7 acceptance criteria |

---

# 14. Success Metrics

## Functional Metrics

| Metric | Target |
|---|---|
| DNA decode | <100ms |
| Forward reconstruction | <5s |
| Reverse analysis | <30s |
| Replay recovery | <500ms |

---

## Determinism Metrics

| Metric | Target |
|---|---|
| repeated encode equality | 100% |
| replay equivalence | 100% |
| registry reproducibility | 100% |
| mutation rollback accuracy | 100% |

---

## Dataset Metrics

| Metric | Minimum | Recommended |
|---|---|---|
| Spring Boot CMS projects | 20-30 | 100-300 |
| semantic nodes | 2k-5k | 50k+ |
| mutation trajectories | 500+ | 10k+ |

---

# 15. Post-MVP Expansion

Deferred capabilities:

- distributed runtime
- semantic clustering at scale
- Kubernetes deployment
- cloud-native orchestration
- multi-language pipelines
- autonomous semantic optimization
- visual semantic graph editor
- IntelliJ plugin
- advanced LLM orchestration

---

# 16. Final Roadmap Principle

Every roadmap phase must preserve:

- semantic determinism
- canonical graph structure
- reproducible replay
- stable public contracts

Feature growth must never compromise semantic consistency.