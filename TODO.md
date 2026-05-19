# SEDNA — Implementation TODO

Execution checklist for AI agents and engineers. Follow order strictly unless a parallel rule is explicitly stated.

**Contract documents:**
- Detailed design: `docs/sedna_detailed_design.md` (v1.1; FR IDs are stable — append only, see §2.4 numbering policy)
- Agent rules: `AGENTS.md`
- Roadmap: `ROADMAP.md`

**Legend:** `[ ]` pending · `[x]` done · `P0` blocks release · `P1` required for phase · `P2` nice-to-have

---

## Phase 0 — Foundation (Weeks 1–2)

### P0 — Repository skeleton

- [ ] Create Gradle Kotlin DSL multi-project `settings.gradle.kts` with modules: `sedna-core`, `sedna-dna`, `sedna-registry`, `sedna-validation`, `sedna-forward`, `sedna-reverse`, `sedna-runtime`, `sedna-mutation`, `sedna-training`, `sedna-persistence`, `sedna-cli`, `examples`, `tests`, `benchmarks`
- [ ] Add `gradle.properties` semantic version per module (start `0.0.1-SNAPSHOT`)
- [ ] Configure Java 21 toolchain, JUnit 5, Spotless, SpotBugs for all subprojects
- [ ] Add CI workflow: `./gradlew build` + test on push
- [ ] Add root `.gitignore` for `generated/`, `.sdna` scratch outputs, IDE files

### P0 — sedna-core

- [ ] Implement canonical records: `SemanticGraph`, `GenomeNode`, `SemanticLink`, `Contract`, `Constraint`, `ExecutionToken`, `Mutation`, `MutationResult`, `SemanticError`, `RegistryVersion`, `VocabRef`
- [ ] Implement `NodeKind`, `LinkType`, `ErrorCode`, `MutationType` enums (stable ordinal documented)
- [ ] Implement `CanonicalOrdering.comparator()` for nodes, links, contracts
- [ ] Implement `Result<T, SemanticError>` type (no exceptions across boundary)
- [ ] Unit tests: collection ordering stability, comparator tie-breaker by `nodeId`

### P0 — sedna-registry (bootstrap skeleton)

- [ ] Embed core vocabulary resource files
- [ ] Implement `SemanticRegistry` interface + in-memory immutable registry
- [ ] Implement bootstrap step 1: load embedded core vocabulary
- [ ] Unit tests: resolve known `VocabRef`; fail on unknown

### P0 — sedna-validation (skeleton)

- [ ] Define `ValidationEngine` interface + `ValidationReport`
- [ ] Stub validators returning `NOT_IMPLEMENTED` with structured error
- [ ] Wire bootstrap: validation engine initializes before pipelines (per AGENTS)

### P0 — Acceptance (Phase 0)

- [ ] `./gradlew build` green (all modules compile as empty skeletons)
- [ ] DTO immutability enforced (records + unmodifiable lists)
- [ ] Module dependency graph matches design doc (Gradle dependency constraints only; ArchUnit rules run from Cross-cutting)

---

## Phase 1 — DNA Core (Weeks 3–6)

### P0 — sedna-dna

- [ ] Implement TLV encoder/decoder (little-endian, length-prefixed arrays, UTF-8)
- [ ] Implement node header: NodeID, NodeKind, VocabularyVersion, execution profile
- [ ] Implement SHA-256 NodeID generation (first 64 bits, canonical serialization input)
- [ ] Implement `DnaEncoder` / `DnaDecoder` with `Result` error mapping
- [ ] Encoder applies `CanonicalOrdering` before every serialize (FR-dna.02)
- [ ] Implement round-trip tests: `encode(decode(dna)) == dna`
- [ ] Implement golden-byte fixtures from minimal hand-crafted graphs
- [ ] Define `MotifFolder` / `MotifExpander` interfaces only (implementation deferred to Phase 3 per ROADMAP)

### P0 — sedna-registry (complete decode)

- [ ] Implement registry extension TLV decode (bootstrap step 3–4)
- [ ] Version pinning on `SemanticGraph.vocabularyVersion`

### P0 — sedna-validation (graph + DNA)

- [ ] Validate topology: no orphan nodes, valid link endpoints
- [ ] Validate NodeID consistency on decode
- [ ] Validate vocabulary references resolvable

### P1 — Benchmarks

- [ ] JMH: encode/decode p95 on reference-sized graph (<100ms target)

### P0 — Acceptance (Phase 1 / v0.1)

- [ ] 100% byte-identical re-encode across 100 iterations
- [ ] NodeID stable across JVM restarts
- [ ] `registry.resolve(coreRef)` never null for embedded refs

---

## Phase 2 — Forward Pipeline (Weeks 7–11)

Phase 2 runs alone in this window. **Do not start Phase 3 at Week 7** — reverse requires Phase 1 complete plus contract resolution (see Phase 3 start window).

### P0 — sedna-forward stages

- [ ] Step 1: DNA parsing (delegate `sedna-dna`)
- [ ] Step 2: Registry resolution
- [ ] Step 3: Hypergraph construction (LINKS remain semantic refs)
- [ ] Step 4: Contract resolution (materialize edges)
- [ ] Step 5: Constraint propagation (hard fail + rollback hook)
- [ ] Step 6: Execution planning (topological sort + NodeID tie-break)
- [ ] Step 7: Code generation (JavaPoet structure, Mustache templates)

### P0 — sedna-forward LLM boundary

- [ ] OpenRouter HTTP client (`SEDNA_LLM_BASE_URL`, `OPENROUTER_API_KEY`, `SEDNA_LLM_MODEL`); disabled by default in CI
- [ ] Empty method body skeleton fallback on LLM failure
- [ ] Tests: structure hash identical with LLM off

### P0 — sedna-cli (minimal)

- [ ] Commands: `forward`, `decode`, `encode`, `validate`
- [ ] Non-zero exit on `SemanticError`

### P0 — examples/cms-reference

- [ ] Add reference Spring Boot CMS project under `examples/cms-reference`
- [ ] Hand-author minimal DNA fixture at `examples/cms-reference-fixture.sdna` for forward tests until reverse exists

**Minimum fixture specification (canonical, shared by Phase 2 and Phase 3 equivalence tests):**

| Element | Requirement |
|---------|-------------|
| Nodes | 1× `ENTITY`, 1× `SERVICE`, 1× `CONTROLLER` |
| Links | 1× `DEPENDENCY` (CONTROLLER → SERVICE or SERVICE → ENTITY per CMS pattern) |
| Contracts | 1× `PROVIDES` / `REQUIRES` pair on SERVICE |
| Constraints | Optional empty list |
| Encoding | SEDNA-BIN-v1 TLV; `CanonicalOrdering` applied before encode |
| Registry | Embedded core vocabulary version only |

- [ ] Document fixture node IDs and bytes in `examples/cms-reference-fixture.README.md` (golden hash for CI)

### P0 — Acceptance (Phase 2 / v0.2)

- [ ] `sedna forward --input=examples/cms-reference-fixture.sdna --output=generated` compiles
- [ ] Identical generated file tree hash (LLM disabled) across 10 runs
- [ ] Validation runs before any file write

---

## Phase 3 — Reverse Pipeline (Weeks 9–13, parallel with Phase 4)

**Start window:** Week **9** earliest (not Week 7). Prerequisites: Phase 1 complete; `SemanticGraph` DTO frozen; contract resolution operational in `sedna-validation` (Phase 2 forward codegen **not** required). Fixture: `examples/cms-reference-fixture.sdna` must exist (produced in Phase 2).

**Parallel rule:** May overlap Phase 4 (Weeks 12–13) after reverse extraction stabilizes; do not block runtime on full reverse completion.

### P0 — sedna-reverse stages

- [ ] Step 1: Source parsing (Spoon primary, JavaParser helper, ASM bytecode, Gradle API)
- [ ] Step 2: Structural graph construction (Tarjan SCC for cycles)
- [ ] Step 3: Semantic extraction (vocabulary matching, UNKNOWN nodes)
- [ ] Step 4: Contract reconstruction
- [ ] Step 5: Motif detection/folding (SEDNA-FOLD-v1 rules)
- [ ] Step 6: Context reconstruction (LOCAL/MODULE/DOMAIN/ISOLATED)
- [ ] Step 7: Genome serialization via `sedna-dna`
- [ ] Step 8: Git trajectory extraction (JGit, atomic deltas)

### P0 — sedna-reverse LLM

- [ ] Optional enrichment: labels only; never topology/contracts
- [ ] UNKNOWN remains if enrichment fails

### P0 — sedna-cli

- [ ] Add `reverse` command → `<project>.sdna`

### P0 — Acceptance (Phase 3 / v0.3)

- [ ] `reverse(examples/cms-reference)` produces deterministic DNA bytes
- [ ] `reverse(forward(examples/cms-reference-fixture.sdna))` passes semantic equivalence suite:
  - identical node count and NodeID set
  - identical contract and constraint sets
  - equivalent motif expansion
  - equivalent dependency topology

---

## Phase 4 — Runtime Engine (Weeks 12–15, overlaps Phase 3 end)

### P0 — sedna-runtime (DAG only for MVP)

- [ ] Implement `RuntimeScheduler.build` with same ordering as forward plan
- [ ] Implement DAG executor on Project Reactor with canonical sequencing
- [ ] Reject STATEFUL/SUPERVISOR profiles with explicit `SemanticError`
- [ ] Compensation hooks: no-op placeholders only (SUPERVISOR deferred; see FR-rt.05)

### P0 — sedna-persistence

- [ ] PostgreSQL schema: append-only checkpoint log
- [ ] Store `ExecutionToken`, snapshot reference (SEDNA-BIN-v1 TLV bytes), sequence number
- [ ] Integration tests using shared Testcontainers PostgreSQL profile (see Cross-cutting)

### P0 — Replay

- [ ] Replay harness excluding timestamps, random, external HTTP
- [ ] Golden trace comparison tests (PostgreSQL via shared Testcontainers profile)

### P0 — sedna-cli

- [ ] Add `run` command

### P0 — Acceptance (Phase 4 / v0.4)

- [ ] Checkpoint restore resumes identical execution order
- [ ] Replay trace hash 100% match on reference graph
- [ ] Runtime scheduling p95 <50ms on reference graph

---

## Phase 5 — Mutation + Advanced Validation (Weeks 16–19)

### P0 — sedna-mutation

- [ ] Implement subtree-scoped mutations: insert, delete, replace, motif fold/unfold, contract upgrade
- [ ] Transaction: apply → validate → commit/rollback
- [ ] Forbid cross-domain rewrites

### P0 — sedna-validation (advanced)

- [ ] Semantic equivalence checker
- [ ] Mutation safety rules
- [ ] Codegen probe hook (compile generated stub)

### P0 — Acceptance (Phase 5 / v0.5)

- [ ] Invalid mutation always rolls back to prior valid graph
- [ ] Deterministic mutation ordering
- [ ] Equivalence verified after valid mutation

---

## Phase 6 — Training Pipeline (Weeks 20–23)

### P0 — sedna-training

- [ ] Project folder ingestion (never whole-repo merge)
- [ ] Reuse reverse stages for per-commit graphs
- [ ] Trajectory construction with ordered commits
- [ ] Deterministic embedding generation
- [ ] Mutation dataset generation
- [ ] Registry update proposal flow (deterministic conflict resolution)

### P0 — sedna-cli

- [ ] Add `train --projects=list.txt`

### P0 — Acceptance (Phase 6 / v0.6)

- [ ] Identical Git history → identical trajectories and embeddings
- [ ] Minimum dataset path documented (20–30 projects)

---

## Phase 7 — Stabilization (Weeks 24–25)

### P0 — Quality gates

- [ ] JMH suite for all performance targets in README
- [ ] Fuzz tests for mutation + encoding
- [ ] Determinism stress tests (parallel runs, multiple JVM forks)
- [ ] Module README files with one working example per public API

### P0 — sedna-cli

- [ ] Stabilize CLI UX, `--help`, consistent error printing

### P0 — Acceptance (v1.0)

- [ ] All phase acceptance criteria pass
- [ ] Public API frozen post-v0.2 forward (additive-only changes)
- [ ] No SpotBugs high-priority issues
- [ ] `docs/sedna_detailed_design.md` traceability updated if APIs drift

---

## Cross-cutting — Always on

### P0 — Determinism guards

- [ ] CI job: byte equality encode/decode
- [ ] CI job: forward output tree hash (LLM off)
- [ ] Ban `HashMap`/`HashSet` iteration in `sedna-dna`, `sedna-forward`, `sedna-reverse`, `sedna-runtime` (ArchUnit or Checkstyle)

### P0 — Architecture tests (always on; enforce from Phase 2 onward)

- [ ] ArchUnit: `sedna-forward` and `sedna-reverse` must not depend on each other
- [ ] ArchUnit: no module defines DTOs outside `sedna-core` (only `sedna-core` exports graph types)
- [ ] ArchUnit: pipeline modules depend on `sedna-core`, `sedna-dna`, `sedna-registry`, `sedna-validation` — not on each other's internals
- [ ] CI fails if ArchUnit rules break (active once `sedna-forward` or `sedna-reverse` has production code)

### P0 — Security

- [ ] No dynamic bytecode execution
- [ ] No shell exec from LLM outputs
- [ ] LLM HTTP client timeouts and size limits

### P1 — Developer experience

- [ ] `CONTRIBUTING.md` with bootstrap order and module graph
- [ ] Docker Compose for PostgreSQL (runtime dev)
- [ ] Shared Testcontainers PostgreSQL profile (`tests` or `sedna-persistence`) for **all** modules requiring DB (`sedna-persistence`, `sedna-runtime` replay, Phase 4+)
- [ ] Sample `application.yml` / env docs for OpenRouter (`https://openrouter.ai/api/v1`)

### P2 — Post-MVP backlog (do not implement before v1.0)

- [ ] STATEFUL runtime profile
- [ ] SUPERVISOR runtime profile
- [ ] IntelliJ plugin
- [ ] Graph visualization UI
- [ ] Distributed runtime / Kafka
- [ ] FAISS vector index (optional post-MVP)
- [ ] Multi-language pipelines

---

## Current repository status (2026-05-19)

- [x] Specification suite v1 (docs/)
- [x] `AGENTS.md`, `README.md`, `ROADMAP.md`
- [x] Detailed design document (`docs/sedna_detailed_design.md`)
- [ ] **No Java implementation modules yet** — start Phase 0

---

## Agent execution notes

1. Implement modules in dependency order: `sedna-core` → `sedna-dna` → `sedna-registry` → `sedna-validation` → `sedna-forward` (Weeks 7–11) → `sedna-reverse` (from Week 9, not Week 7) ∥ `sedna-runtime` (from Week 12) → `sedna-mutation` → `sedna-training` → `sedna-cli`.
2. Never duplicate DTOs outside `sedna-core`.
3. Every public method returns `Result<T, SemanticError>` at module boundaries.
4. Mark tasks `[x]` only when tests and CI gates for that task pass.
