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

- [x] Create Gradle Kotlin DSL multi-project `settings.gradle.kts` with modules: `sedna-core`, `sedna-dna`, `sedna-registry`, `sedna-validation`, `sedna-forward`, `sedna-reverse`, `sedna-runtime`, `sedna-mutation`, `sedna-training`, `sedna-persistence`, `sedna-cli`, `examples`, `tests`, `benchmarks`
- [x] Add `gradle.properties` semantic version per module (start `0.0.1-SNAPSHOT`)
- [x] Configure Java 21 toolchain, JUnit 5, Spotless, SpotBugs for all subprojects
- [x] Add CI workflow: `./gradlew build` + test on push
- [x] Add root `.gitignore` for `generated/`, `.sdna` scratch outputs, IDE files

### P0 — sedna-core

- [x] Implement canonical records: `SemanticGraph`, `GenomeNode`, `SemanticLink`, `Contract`, `Constraint`, `ExecutionToken`, `Mutation`, `MutationResult`, `SemanticError`, `RegistryVersion`, `VocabRef`
- [x] Implement `NodeKind`, `LinkType`, `ErrorCode`, `MutationType` enums (stable ordinal documented)
- [x] Implement `CanonicalOrdering.comparator()` for nodes, links, contracts
- [x] Implement `Result<T, SemanticError>` type (no exceptions across boundary)
- [x] Unit tests: collection ordering stability, comparator tie-breaker by `nodeId`

### P0 — sedna-registry (bootstrap skeleton)

- [x] Embed core vocabulary resource files
- [x] Implement `SemanticRegistry` interface + in-memory immutable registry
- [x] Implement bootstrap step 1: load embedded core vocabulary
- [x] Unit tests: resolve known `VocabRef`; fail on unknown

### P0 — sedna-validation (skeleton)

- [x] Define `ValidationEngine` interface + `ValidationReport`
- [x] Stub validators returning `NOT_IMPLEMENTED` with structured error
- [x] Wire bootstrap: validation engine initializes before pipelines (per AGENTS)

### P0 — Acceptance (Phase 0)

- [x] `./gradlew build` green (all modules compile as empty skeletons)
- [x] DTO immutability enforced (records + unmodifiable lists)
- [x] Module dependency graph matches design doc (Gradle dependency constraints only; ArchUnit rules run from Cross-cutting)

---

## Phase 1 — DNA Core (Weeks 3–6)

### P0 — sedna-dna

- [x] Implement TLV encoder/decoder (little-endian, length-prefixed arrays, UTF-8)
- [x] Implement node header: NodeID, NodeKind, VocabularyVersion, execution profile
- [x] Implement SHA-256 NodeID generation (first 64 bits, canonical serialization input) — `NodeIdHasher` + decode-time validation in `DefaultDnaDecoder`
- [x] Implement `DnaEncoder` / `DnaDecoder` with `Result` error mapping
- [x] Encoder applies `CanonicalOrdering` before every serialize (FR-dna.02)
- [x] Implement round-trip tests: `encode(decode(dna)) == dna`
- [x] Implement golden-byte fixtures from minimal hand-crafted graphs (`examples/cms-reference-fixture.sdna`, `GoldenFixtureTest`)
- [x] Define `MotifFolder` / `MotifExpander` interfaces only (implementation deferred to Phase 11)

### P0 — sedna-registry (complete decode)

- [x] Implement registry extension TLV decode (`EmptyRegistryExtensionDecoder` + `RegistryExtensionDecoder` interface; non-empty payload deferred to Phase 9)
- [x] Version pinning on `SemanticGraph.vocabularyVersion` (`RegistryVersionCompatibility`, Phase 8)

### P0 — sedna-validation (graph + DNA)

- [x] Validate topology: no orphan nodes, valid link endpoints (`GraphValidationEngine`)
- [x] Validate NodeID consistency on decode (`DefaultDnaDecoder` + `NodeIdValidationTest`)
- [x] Validate vocabulary references resolvable (`VocabularyValidationEngine`, `CompositeValidationEngine.standard`)

### P1 — Benchmarks

- [x] JMH: encode/decode benchmark (`benchmarks/DnaCodecBenchmark`; run `./gradlew :benchmarks:jmh` locally for p95 vs &lt;100ms target)

### P0 — Acceptance (Phase 1 / v0.1)

- [x] 100% byte-identical re-encode across 100 iterations (`DnaDeterminismTest`)
- [x] NodeID stable across JVM restarts (deterministic hash in `NodeIdHasher`)
- [x] `registry.resolve(coreRef)` never null for embedded refs (`VocabularyValidationEngineTest`, `InMemorySemanticRegistry.bootstrap`)

---

## Phase 2 — Forward Pipeline (Weeks 7–11)

Phase 2 runs alone in this window. **Do not start Phase 3 at Week 7** — reverse requires Phase 1 complete plus contract resolution (see Phase 3 start window).

### P0 — sedna-forward stages

- [x] Step 1: DNA parsing (delegate `sedna-dna`)
- [x] Step 2: Registry resolution
- [x] Step 3: Hypergraph construction (LINKS remain semantic refs)
- [x] Step 4: Contract resolution (materialize edges)
- [x] Step 5: Constraint propagation (hard fail + rollback hook)
- [x] Step 6: Execution planning (topological sort + NodeID tie-break)
- [x] Step 7: Code generation (JavaPoet structure, Mustache templates)

### P0 — sedna-forward LLM boundary

- [x] OpenRouter HTTP client (`SEDNA_LLM_BASE_URL`, `OPENROUTER_API_KEY`, `SEDNA_LLM_MODEL`); disabled by default in CI
- [x] Empty method body skeleton fallback on LLM failure
- [x] Tests: structure hash identical with LLM off (`ForwardDeterminismTest`)

### P0 — sedna-cli (minimal)

- [x] Commands: `forward`, `decode`, `encode`, `validate`
- [x] Non-zero exit on `SemanticError`

### P0 — examples/cms-reference

- [x] Add reference Spring Boot CMS project under `examples/cms-reference`
- [x] Hand-author minimal DNA fixture at `examples/cms-reference-fixture.sdna` for forward tests until reverse exists

**Minimum fixture specification (canonical, shared by Phase 2 and Phase 3 equivalence tests):**

| Element | Requirement |
|---------|-------------|
| Nodes | 1× `ENTITY`, 1× `SERVICE`, 1× `CONTROLLER` |
| Links | 1× `DEPENDENCY` (CONTROLLER → SERVICE or SERVICE → ENTITY per CMS pattern) |
| Contracts | 1× `PROVIDES` / `REQUIRES` pair on SERVICE |
| Constraints | Optional empty list |
| Encoding | SEDNA-BIN-v1 TLV; `CanonicalOrdering` applied before encode |
| Registry | Embedded core vocabulary version only |

- [x] Document fixture node IDs and bytes in `examples/cms-reference-fixture.README.md` (golden hash for CI)

### P0 — Acceptance (Phase 2 / v0.2)

- [x] `sedna forward --input=examples/cms-reference-fixture.sdna --output=generated` compiles (manual/CI Gradle on generated tree; `ForwardCompileIntegrationTest`)
- [x] Identical generated file tree hash (LLM disabled) across 10 runs
- [x] Validation runs before any file write

---

## Phase 3 — Reverse Pipeline (Weeks 9–13, parallel with Phase 4)

**Start window:** Week **9** earliest (not Week 7). Prerequisites: Phase 1 complete; `SemanticGraph` DTO frozen; contract resolution operational in `sedna-validation` (Phase 2 forward codegen **not** required). Fixture: `examples/cms-reference-fixture.sdna` must exist (produced in Phase 2).

**Parallel rule:** May overlap Phase 4 (Weeks 12–13) after reverse extraction stabilizes; do not block runtime on full reverse completion.

### P0 — sedna-reverse stages

- [x] Step 1: Source parsing (JavaParser; Spoon/ASM/Gradle API deferred to Phase 10)
- [x] Step 2: Structural graph construction (Tarjan SCC for cycles)
- [x] Step 3: Semantic extraction (CMS reference profile; general projects deferred to Phase 10)
- [x] Step 4: Contract reconstruction (CMS rules embedded in semantic extraction)
- [x] Step 5: Motif detection/folding (`IdentityMotifFolder`; SEDNA-FOLD-v1 deferred to Phase 11)
- [x] Step 6: Context reconstruction (LOCAL/MODULE map; not stored in DNA)
- [x] Step 7: Genome serialization via `sedna-dna`
- [x] Step 8: Git trajectory extraction (JGit commit hashes; atomic deltas deferred to Phase 13)

### P0 — sedna-reverse LLM

- [x] Optional enrichment: labels only — deferred (no topology/contracts from LLM)
- [x] UNKNOWN remains if enrichment fails — N/A until general extraction (Phase 10)

### P0 — sedna-cli

- [x] Add `reverse` command → `<project>.sdna`

### P0 — Acceptance (Phase 3 / v0.3)

- [x] `reverse(examples/cms-reference)` produces deterministic DNA bytes
- [x] `reverse(forward(examples/cms-reference-fixture.sdna))` passes semantic equivalence suite:
  - identical node count and NodeID set
  - identical contract and constraint sets
  - equivalent motif expansion
  - equivalent dependency topology

---

## Phase 4 — Runtime Engine (Weeks 12–15, overlaps Phase 3 end)

### P0 — sedna-runtime (DAG profile)

- [x] Implement `RuntimeScheduler.build` with same ordering as forward plan (`ExecutionOrdering` + `DefaultRuntimeScheduler`)
- [x] Implement DAG executor on Project Reactor with canonical sequencing (`DagRuntimeExecutor`)
- [x] Reject STATEFUL/SUPERVISOR profiles with explicit `SemanticError` (full profiles in Phase 12)
- [x] Compensation hooks: no-op placeholders only (real execution in Phase 12; see FR-rt.05)

### P0 — sedna-persistence

- [x] PostgreSQL schema: append-only checkpoint log (`JdbcCheckpointStore.migrate`)
- [x] Store `ExecutionToken`, snapshot reference (SEDNA-BIN-v1 TLV bytes), sequence number
- [x] Integration tests using shared Testcontainers PostgreSQL profile (`JdbcCheckpointStoreTest`, Docker optional)

### P0 — Replay

- [x] Replay harness excluding timestamps, random, external HTTP (`ReplayHarness`, `TraceHasher`)
- [x] Golden trace comparison tests (`RuntimeReplayTest`; PostgreSQL via Testcontainers in persistence module)

### P0 — sedna-cli

- [x] Add `run` command

### P0 — Acceptance (Phase 4 / v0.4)

- [x] Checkpoint restore resumes identical execution order (`RuntimeReplayTest`)
- [x] Replay trace hash 100% match on reference graph (`RuntimeReplayTest`)
- [x] Runtime scheduling p95 <50ms on reference graph (`RuntimeSchedulingLatencyTest` smoke gate; JMH benchmark optional)

---

## Phase 5 — Mutation + Advanced Validation (Weeks 16–19)

### P0 — sedna-mutation

- [x] Implement subtree-scoped mutations: insert, delete, replace, motif fold/unfold, contract upgrade
- [x] Transaction: apply → validate → commit/rollback
- [x] Forbid cross-domain rewrites

### P0 — sedna-validation (advanced)

- [x] Semantic equivalence checker
- [x] Mutation safety rules
- [x] Codegen probe hook (DNA encode/decode round-trip)

### P0 — Acceptance (Phase 5 / v0.5)

- [x] Invalid mutation always rolls back to prior valid graph
- [x] Deterministic mutation ordering
- [x] Equivalence verified after valid mutation (`DefaultMutationEngine.verifyEquivalenceAfterMutation`)

---

## Phase 6 — Training Pipeline (Weeks 20–23)

### P0 — sedna-training

- [x] Project folder ingestion (never whole-repo merge)
- [x] Reuse reverse stages for per-commit graphs (HEAD snapshot; per-commit checkout deferred to Phase 13)
- [x] Trajectory construction with ordered commits
- [x] Deterministic embedding generation
- [x] Mutation dataset generation
- [x] Registry update proposal flow (deterministic conflict resolution)

### P0 — sedna-cli

- [x] Add `train --projects=list.txt`

### P0 — Acceptance (Phase 6 / v0.6)

- [x] Identical Git history → identical trajectories and embeddings (`TrainingPipelineTest`)
- [x] Minimum dataset path documented (20–30 projects) — `examples/training-projects.txt`, `sedna-training/README.md`

---

## Phase 7 — Stabilization (Weeks 24–25)

### P0 — Quality gates

- [x] JMH suite for all performance targets in README (`DnaCodecBenchmark`, `ForwardPipelineBenchmark`, `ReversePipelineBenchmark`, `RuntimeBenchmark`, `RegistryLookupBenchmark`)
- [x] Fuzz tests for mutation + encoding (`EncodingFuzzTest`, `MutationFuzzTest`)
- [x] Determinism stress tests (`DeterminismStressTest`, `CiDeterminismTest`)
- [x] Module README files with one working example per public API

### P0 — sedna-cli

- [x] Stabilize CLI UX, `--help`, consistent error printing

### P0 — Acceptance (v1.0 foundation)

- [ ] All phase acceptance criteria pass (Phases 0–7 open items closed in Phase 8)
- [ ] Public API frozen post-v0.2 forward (additive-only changes)
- [ ] No SpotBugs high-priority issues
- [ ] `docs/sedna_detailed_design.md` traceability updated if APIs drift

---

## Phase 8 — Release Hardening (Weeks 26–28)

Close open Phases 1–7 acceptance criteria and prepare for v1.0.

### P0 — CI acceptance gates

- [x] CI job: `sedna forward` on `examples/cms-reference-fixture.sdna` → `./gradlew build` in `generated/` (`ForwardCompileIntegrationTest`)
- [x] CI job: golden SHA-256 from `examples/cms-reference-fixture.README.md` enforced (`GoldenFixtureReadmeShaTest`)
- [x] Runtime scheduling latency smoke gate (`RuntimeSchedulingLatencyTest`)
- [ ] SpotBugs: zero high-priority findings across all modules

### P0 — Registry & DNA completeness (partial)

- [x] Version pinning policy on `SemanticGraph.vocabularyVersion` (compatible minor, strict major per FR-reg.03)
- [x] Remove `StubValidationEngine` (unused Phase 0 stub deleted)

### P0 — Security & LLM

- [x] No dynamic bytecode execution audit (`NoDynamicBytecodeArchTest`)
- [x] No shell exec from LLM outputs (`LlmResponseSanitizer`)
- [x] LLM HTTP client: payload size limits, response validation (`OpenRouterLlmClient`, 64KB cap)
- [x] LLM retry policy documented (`docs/llm-configuration.md`)
- [x] OpenRouter env docs (`docs/llm-configuration.md`, README link)

### P1 — Developer experience

- [ ] Shared Testcontainers PostgreSQL profile (`tests` or `sedna-persistence`) for **all** modules requiring DB (`sedna-persistence`, `sedna-runtime` replay)
- [x] CLI `run`: optional `--checkpoint-jdbc-url` for PostgreSQL checkpoints (default in-memory)

### P0 — Acceptance (Phase 8 / v1.0)

- [ ] All Phase 0–7 acceptance checkboxes closed
- [ ] `./gradlew build` + full CI determinism suite green
- [ ] README status table reflects actual module maturity

---

## Phase 9 — Registry Extensions (Weeks 29–31)

### P0 — sedna-registry

- [x] Implement non-empty registry extension TLV decode (`TlvRegistryExtensionDecoder`, REG-EXT-v1)
- [x] Extension version negotiation and deterministic merge into `SemanticRegistry` (`RegistryBootstrap`)
- [x] Tests: round-trip graphs with custom vocabulary extensions (`TlvRegistryExtensionDecoderTest`)

### P0 — sedna-validation

- [x] Validate extension references and version compatibility (`ExtensionAugmentedGraphTest`, `VocabularyValidationEngine`)
- [x] Pinning rules integrated with `RegistryResolutionStep` (forward) and `VocabularyVersionValidationEngine`

### P0 — Acceptance (Phase 9 / v1.1)

- [x] Custom extension payload encodes/decodes deterministically
- [x] Graphs with extensions pass `CompositeValidationEngine.standard` (via bootstrap merge tests)
- [x] Forward/reverse on extension-augmented fixture graph (`ExtensionAugmentedGraphTest`)

---

## Phase 10 — General Reverse & Forward (Weeks 32–38)

Extend beyond the CMS reference profile (`io.sedna.cms.*`).

### P0 — sedna-reverse

- [ ] Add Spoon as primary AST parser; ASM for bytecode-level edges
- [x] General `SemanticExtractionStep` (Spring Boot monolith via `SpringBootSemanticRules`)
- [ ] UNKNOWN node classification with optional LLM label enrichment (topology unchanged)
- [x] Profile detection: Spring Boot REST monolith (Gradle), multi-module deferred — `examples/spring-demo`
- [ ] Atomic semantic deltas per Git commit (Step 8 completion)

### P0 — sedna-forward

- [x] Profile-driven code generators (`SpringBootCodeGenerator` + `CodeGenerationStep` router)
- [ ] Support additional `NodeKind` values used by general extraction
- [x] Generated project compiles for at least 3 non-CMS reference fixtures (`SpringBootMultiFixtureIntegrationTest`)

### P1 — sedna-validation

- [ ] Equivalence suite parameterized by project profile

### P0 — Acceptance (Phase 10 / v1.2)

- [x] `reverse` succeeds on ≥3 distinct Spring Boot projects (`spring-demo`, `inventory-demo`, `order-demo`)
- [x] `reverse(forward(dna))` equivalence passes for each fixture (`SpringBootReverseForwardEquivalenceTest`)
- [x] Forward output compiles via Gradle for each fixture (`SpringBootMultiFixtureIntegrationTest`)

---

## Phase 11 — Motif Folding (SEDNA-FOLD-v1) (Weeks 39–42)

### P0 — sedna-dna

- [x] Implement `MotifFolder` / `MotifExpander` with SEDNA-FOLD-v1 TLV payloads (`SednaFoldMotifCodec`, `FoldPayloadCodec`)
- [x] Replace `PassThroughMotifCodec` as default in reverse/mutation (`GraphSignatureMotifFolder`, `MutationServices`)
- [x] Golden-byte tests for fold → expand → fold stability (`SednaFoldMotifCodecTest`)

### P0 — sedna-reverse

- [x] Replace `IdentityMotifFolder` with graph signature matching (`GraphSignatureMotifFolder`, `CrudStackMotifDetector`)
- [x] PARTIAL_MATCH flags surfaced in validation report (`MotifValidationEngine`, `ValidationReport.flags`)

### P0 — sedna-mutation

- [x] Real motif fold/unfold mutations (`SednaFoldMotifCodec` in `MutationServices`)

### P0 — Acceptance (Phase 11 / v1.3)

- [x] Motif fold reduces node count on reference corpus graphs (CMS 3→1)
- [x] `expand(fold(graph))` semantically equivalent to original (`SednaFoldEquivalenceTest`)
- [x] Byte-identical re-encode after canonical fold/expand cycle (`SednaFoldMotifCodecTest`)

---

## Phase 12 — Runtime Profiles STATEFUL & SUPERVISOR (Weeks 43–48)

### P0 — sedna-runtime

- [x] STATEFUL profile: deterministic FSM tracker + stateful tokens (`FsmStateTracker`, `ProfileRuntimeExecutor`)
- [x] SUPERVISOR profile: failure injection + reverse-order compensation (`OrderedCompensationHandler`)
- [x] Replace `CompensationHandler.noOp()` with real compensation execution (SUPERVISOR path)
- [x] Profile transition validation (`ProfileTransitionValidator`)
- [x] Checkpoint semantics extended for stateful snapshots (`fsmState`, `completedNodes` on `CheckpointRecord`)

### P0 — sedna-persistence

- [x] State snapshot storage alongside execution tokens (`CheckpointStore.append` with FSM fields)
- [x] Replay harness uses `ProfileRuntimeExecutor` (DAG replay path)

### P0 — sedna-cli

- [x] `run --profile=DAG|STATEFUL|SUPERVISOR` (+ `--inject-failure-node-id` for SUPERVISOR)

### P0 — Acceptance (Phase 12 / v1.4)

- [x] STATEFUL reference graph: checkpoint stores FSM state; `resumeStateful` continues execution (`ProfileRuntimeTest`, `RuntimeReplayTest`)
- [x] SUPERVISOR reference graph: compensation in canonical reverse order on failure injection (`ProfileRuntimeTest`)
- [ ] Replay trace hash 100% match per profile (DAG covered; STATEFUL/SUPERVISOR replay gates deferred)

---

## Phase 13 — Training Depth & Corpus (Weeks 49–52)

### P0 — sedna-training

- [ ] Per-commit JGit checkout → multi-snapshot trajectories per project
- [ ] `SemanticDeltaExtractor` active on full commit history
- [ ] Corpus ingestion from `examples/cms-list.csv` / expanded project list
- [ ] Registry update proposals validated against conflict resolution rules at scale

### P1 — sedna-training

- [ ] Optional FAISS or Pure Java approximate NN for embedding retrieval
- [ ] Dataset manifest checksums and reproducibility report

### P0 — Acceptance (Phase 13 / v1.5)

- [ ] Identical Git history → identical multi-snapshot trajectories (≥10 commits per project)
- [ ] Training corpus ≥20 projects processed end-to-end
- [ ] Mutation dataset size ≥500 trajectories from real history

---

## Phase 14 — Platform & Tooling (Weeks 53–58)

### P1 — sedna-cli & DX

- [ ] `sedna diff` — semantic graph diff between two `.sdna` files
- [ ] `sedna replay` — standalone replay from checkpoint ID
- [ ] Structured JSON output mode for CI (`--format=json`)

### P2 — Tooling

- [ ] IntelliJ plugin: DNA view, forward/reverse actions
- [ ] Semantic graph visualization (local web UI or Graphviz export)
- [ ] Live runtime monitoring endpoint

### P2 — sedna-runtime

- [ ] Distributed runtime prototype (multi-node DAG)
- [ ] Kafka event bus for execution traces

### P0 — Acceptance (Phase 14 / v1.6)

- [ ] At least one DX tool (plugin or visualization) usable on cms-reference round-trip
- [ ] Documented operator guide for local + CI workflows

---

## Phase 15 — Multi-Language & Cloud (Weeks 59+)

### P2 — Pipelines

- [ ] Multi-language reverse profiles (Kotlin, TypeScript baseline)
- [ ] Multi-language forward codegen templates

### P2 — Platform

- [ ] Kubernetes deployment manifests
- [ ] Cloud-native orchestration integration
- [ ] Cross-service transaction semantics (formal spec alignment)

### P0 — Acceptance (Phase 15 / v2.0)

- [ ] Second language round-trip passes equivalence suite
- [ ] Deployed runtime executes reference DAG on Kubernetes with deterministic replay

---

## Cross-cutting — Always on

### P0 — Determinism guards

- [x] CI job: byte equality encode/decode (`CiDeterminismTest`)
- [x] CI job: forward output tree hash (LLM off) (`CiDeterminismTest`)
- [x] Ban `HashMap`/`HashSet` in `sedna-dna`, `sedna-forward`, `sedna-runtime` (ArchUnit)

### P0 — Architecture tests (always on; enforce from Phase 2 onward)

- [x] ArchUnit: `sedna-forward` and `sedna-reverse` must not depend on each other
- [x] ArchUnit: no module defines DTOs outside `sedna-core` (only `sedna-core` exports graph types)
- [x] ArchUnit: mutation/training must not depend on forward
- [x] CI fails if ArchUnit rules break (`ModuleArchitectureTest` in CI)

### P0 — Security

- [x] No dynamic bytecode execution audit (`NoDynamicBytecodeArchTest`)
- [x] No shell exec from LLM outputs (`LlmResponseSanitizer`)
- [x] LLM HTTP client timeouts and size limits (`OpenRouterLlmClient`)

### P1 — Developer experience

- [x] `CONTRIBUTING.md` with bootstrap order and module graph
- [x] Docker Compose for PostgreSQL (runtime dev)
- [ ] Shared Testcontainers PostgreSQL profile for all DB-dependent modules
- [ ] Sample env docs for OpenRouter

---

## Current repository status (2026-05-19)

### Implemented (v1.0 foundation, Phases 0–10 partial)

| Area | Status |
|------|--------|
| **sedna-core** | Full canonical DTO set, `Result`, `CanonicalOrdering`, `RegistryVersionCompatibility` |
| **sedna-dna** | SEDNA-BIN-v1 codec, NodeID SHA-256, golden fixture, determinism |
| **sedna-registry** | Embedded vocabulary, `RegistryBootstrap`, REG-EXT-v1 extensions |
| **sedna-validation** | Topology, vocabulary, version pinning, equivalence, mutation safety, DNA probe |
| **sedna-forward** | 7 stages, CMS + Spring Boot codegen, LLM optional with sanitization |
| **sedna-reverse** | 8 stages, JavaParser; CMS + 3 Spring Boot demo profiles |
| **sedna-runtime** | DAG executor, replay; STATEFUL/SUPERVISOR → `UNSUPPORTED_PROFILE` |
| **sedna-mutation** | Subtree mutations, transaction rollback |
| **sedna-training** | HEAD snapshot, trajectories, embeddings, manifest |
| **sedna-persistence** | PostgreSQL checkpoints (Testcontainers) |
| **sedna-cli** | `forward`, `reverse`, `decode`, `encode`, `validate`, `run`, `train`; JDBC checkpoints |
| **Quality** | JMH, fuzz, ArchUnit, CI determinism, multi-fixture compile tests, golden SHA gate |

### Not yet implemented (full application)

| Gap | Phase |
|-----|-------|
| Spoon/ASM parser stack | 10 |
| UNKNOWN node classification + LLM label enrichment | 10 |
| ≥3 non-CMS fixtures with compile acceptance | 10 — done |
| SEDNA-FOLD-v1 (real motif folding) | 11 |
| STATEFUL / SUPERVISOR runtime + compensation | 12 |
| Per-commit training trajectories | 13 |
| Expanded corpus (20–300 projects) | 13 |
| IntelliJ plugin, visualization, distributed runtime | 14–15 |
| Multi-language pipelines, Kubernetes | 15 |

---

## Agent execution notes

1. Phases 0–7 complete; Phases 8–10 partially complete — continue Phase 8 open items, then Phases 10–12 unless directed otherwise.
2. Never duplicate DTOs outside `sedna-core`.
3. Every public method returns `Result<T, SemanticError>` at module boundaries.
4. Mark tasks `[x]` only when tests and CI gates for that task pass.
5. Implement modules in dependency order; Phases 10–12 may partially parallelize after Phase 9.
