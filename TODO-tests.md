# SEDNA E2E Tests — Implementation Checklist

Follow this list in order when implementing the E2E suite.  
Contract document: [`docs/senda_e2e_tests_detailed_design.md`](docs/senda_e2e_tests_detailed_design.md).  
Scenario catalog: [`docs/sedna_e2e_test_plan.md`](docs/sedna_e2e_test_plan.md).

**Conventions**

- Test package: `io.sedna.tests.e2e`
- Output root: `build/test-outputs/<test-id>/` (e.g. `E2E-007`)
- CLI invocations: `./gradlew :sedna-cli:run --args="..."`
- CI: `SEDNA_LLM_ENABLED=false` (default)
- Mark items `[x]` when done

---

## R0 — Harness foundation

- [x] **R0.1** Add `E2eTestSupport` in `tests/src/test/java/io/sedna/tests/e2e/E2eTestSupport.java`
  - `Path outputDir(String testId)` → `build/test-outputs/<test-id>/`
  - `prepareDir(Path)` — delete + recreate (until CLI `--clean` exists)
  - `runCli(String... args)` — ProcessBuilder on `:sedna-cli:run`, capture exit code + stdout
  - `readGoldenFixture()` — via `ExamplesLayout.goldenCmsFixture(RepoPaths.locateRoot())`
  - `treeHash(Map<String,String>)` — copy from `CiDeterminismTest`
  - `assertEnvLlmDisabled()` — fail if `SEDNA_LLM_ENABLED=true`

- [x] **R0.2** Register Gradle `e2e` task in `tests/build.gradle.kts`
  ```kotlin
  tasks.register<Test>("e2e") {
      description = "End-to-end integration tests"
      group = "verification"
      useJUnitPlatform {
          includeTags("e2e")
      }
      environment("SEDNA_LLM_ENABLED", "false")
  }
  ```
  Wire root `build.gradle.kts` optional: `check` dependsOn `e2e` (or document as separate CI job).

- [x] **R0.3** Add JUnit tag `@Tag("e2e")` to all classes under `io.sedna.tests.e2e`.

- [x] **R0.4** Document in `README.md` and `docs/operator-guide.md`:
  ```bash
  ./gradlew e2e
  ```

- [x] **R0.5** Add `.gitignore` entry if needed: `build/test-outputs/` (optional — may keep for local debug).

---

## R1 — DNA, registry, validation (E2E-001 – E2E-005, E2E-012 – E2E-013)

- [x] **E2E-001** `DnaEncodeE2eTest` — encode `CmsReferenceFixtureGraph`, assert SHA-256 = `6d75d8431baaac07398e39448b19db34b62cc6df7eb84cbdc1484d5c2d7ed8f5`, write to isolated dir.

- [x] **E2E-002** `DnaDecodeE2eTest` — decode golden fixture; assert `nodes=3`, `links=1`, registry `core:1.0`.

- [x] **E2E-003** `DnaRoundtripE2eTest` — triple-run byte identity `encode(decode(x))==x`; migrate logic from `CiDeterminismTest` or delegate.

- [x] **E2E-004** `RegistryBootstrapE2eTest` — bootstrap registry; resolve all contracts on golden graph; no unresolved refs.

- [x] **E2E-005** `RegistryConflictE2eTest`
  - [x] Create `tests/fixtures/registry/conflicts/` (programmatic via `E2eFixtures`) with deterministic conflicting extension payloads
  - [x] Assert conflict report order stable across two runs

- [x] **E2E-012** `ValidateGoldenE2eTest` — CLI `validate --input=<golden> --format=json` exit 0.

- [x] **E2E-013** `ValidateInvalidE2eTest`
  - [x] Add `tests/fixtures/invalid/invalid-graph.sdna` (generated on first run) (and variants per validation rules)
  - [x] CLI validate exit 1; assert first `ErrorCode` stable

---

## R2 — Forward & reverse pipelines (E2E-006 – E2E-011, chain §15)

- [x] **E2E-006** `ForwardGenerateE2eTest` — CLI forward to `build/test-outputs/E2E-006/generated/`; assert `build.gradle.kts` exists.

- [x] **E2E-007** Refactor `ForwardCompileIntegrationTest` → `ForwardCompileE2eTest` in `e2e` package; use `E2eTestSupport.outputDir("E2E-007")`; tag `@Tag("e2e")`.

- [x] **E2E-008** `ForwardDeterminismE2eTest` — two forward runs, identical `treeHash`; LLM off.

- [x] **E2E-009** `ReverseAnalysisE2eTest` — CLI reverse on `examples/sedna-cms/cms-reference`; output non-empty `.sdna`.

- [x] **E2E-010** Keep `SpringBootReverseForwardEquivalenceTest`; add `@Tag("e2e")` or thin wrapper `ReverseForwardEquivalenceE2eTest` calling same logic for cms-reference + golden fixture path.

- [x] **E2E-011** Skip with `@Disabled("extract-trajectories CLI not implemented")` and reference in test plan — **or** implement when CLI exists.

- [x] **Chain** `CoreValidationChainE2eTest` — single test executing §15 sequence (encode → decode → forward → compile → reverse → equivalence).

---

## R3 — Runtime & mutation (E2E-014 – E2E-019, E2E-019B, E2E-029 – E2E-030)

- [x] **E2E-014** `RuntimeDagE2eTest` — CLI `run --input=<golden> --format=json`; parse `traceSha256`; assert event order matches API `RuntimeServices.engine().run`.

- [x] **E2E-015** `RuntimeReplayE2eTest` — run with `--checkpoint-dir`; replay; assert identical `traceSha256`.

- [x] **E2E-016** `RuntimeRecoveryE2eTest` — `--inject-failure-node-id`; recover via checkpoint + replay.

- [x] **E2E-017** `MutationApplyE2eTest`
  - [x] Add `tests/fixtures/mutations/add-payment-module.json`
  - [x] API `MutationEngine.apply`; validate output

- [x] **E2E-018** `MutationRollbackE2eTest` — snapshot rollback restores pre-mutation graph (equivalence or bytes).

- [x] **E2E-019** `MutationRejectE2eTest` — `tests/fixtures/mutations/invalid-cross-domain.json` on valid DNA → rejected.

- [x] **E2E-019B** `DeepMutationDriftE2eTest` — 10 sequential valid mutations; final forward compiles.

- [x] **E2E-029** `RegistryRecoveryE2eTest` — corrupted registry fixture; bootstrap recovery; deterministic state.

- [x] **E2E-030** `InterruptedRuntimeE2eTest` — simulate interrupt; resume from checkpoint; final trace hash stable.

---

## R4 — Training & CLI (E2E-020 – E2E-025, E2E-023)

- [x] **E2E-020** `TrainingDatasetE2eTest` — `sedna train --corpus=<repoRoot> --output=<dir> --format=json`; assert `dataset.manifest` + `.sha256` exist; fingerprint stable across 2 runs.

- [x] **E2E-021** `MotifDiscoveryE2eTest` — API-level motif discovery on training output (until CLI subcommand exists).

- [x] **E2E-022** `EmbeddingStabilityE2eTest` — cosine ≥ 0.9999, ε ≤ 1e-6; **no** byte hash on float vectors.

- [x] **E2E-023** `RegistryLearningE2eTest` — registry extension list ordering stable (TreeMap / canonical sort).

- [x] **E2E-024** `CliHelpE2eTest` — `help` exit 0; stdout contains `forward`, `reverse`, `validate`.

- [x] **E2E-025** `CliInvalidArgsE2eTest` — unknown option exit 2; message stable (normalize line endings).

---

## R5 — Benchmarks & full determinism (E2E-026 – E2E-031)

- [x] **E2E-026** Document JMH gate: `./gradlew jmh -Pincludes=DnaCodecBenchmark` — decode p99 < 100 ms (warmup per JMH defaults).

- [x] **E2E-027** JMH `ForwardPipelineBenchmark` p99 < 5 s.

- [x] **E2E-028** JMH `ReversePipelineBenchmark` p99 < 30 s on `cms-reference`.

- [x] **E2E-031** `FullDeterminismSuite` — `@Tag("e2e")` class running in one JVM:
  - golden SHA
  - forward treeHash ×2
  - validate golden
  - replay trace hash
  - optional: train fingerprint

- [ ] **CI split** — PR: `./gradlew e2e`; main/nightly: `./gradlew jmh` with threshold check script (optional `benchmarks/verify-thresholds.sh`).

---

## Fixtures & data files to add

- [ ] `tests/fixtures/invalid/invalid-graph.sdna`
- [ ] `tests/fixtures/invalid/` — additional cases (orphan node, broken contract, cyclic dependency)
- [ ] `tests/fixtures/registry/conflicts/`
- [ ] `tests/fixtures/mutations/add-payment-module.json`
- [ ] `tests/fixtures/mutations/invalid-cross-domain.json`
- [ ] `tests/fixtures/mutations/sequence-10-valid/` (for E2E-019B)
- [ ] `tests/fixtures/registry/corrupted/` (for E2E-029)

---

## Refactor / migrate existing tests

- [ ] Add `@Tag("e2e")` to: `CiDeterminismTest`, `ForwardCompileIntegrationTest`, `SpringBootReverseForwardEquivalenceTest`, `Phase14AcceptanceTest`, `DeterminismStressTest` (review each for runtime — may stay in `test` vs `e2e`).

- [ ] Decide: keep fuzz tests (`EncodingFuzzTest`, `MutationFuzzTest`) in default `test` task, not `e2e`.

- [ ] Align `GoldenFixtureReadmeShaTest` with `E2E-001` or merge into `DnaEncodeE2eTest`.

---

## Documentation sync (after implementation)

- [ ] Update `docs/sedna_e2e_test_plan.md` paths:
  - `:sedna-tests` → `:tests` / `./gradlew e2e`
  - `examples/dna/` → `examples/sedna-e2e-tests/`
  - `examples/cms-reference` → `examples/sedna-cms/cms-reference`
  - Module `run` → `sedna-cli` commands

- [x] Add E2E section to `docs/operator-guide.md` (run e2e, output dirs, LLM off).

- [ ] Optional: add `examples/sedna-blog-reference` / `shop-reference` **or** remove from test plan (currently use `sedna-demo` only).

---

## Optional enhancements (post-R5)

- [ ] Implement `--clean` on `sedna-cli` forward/reverse/train; switch `E2eTestSupport` to use it.

- [ ] `sedna-cli` subcommands: `mutate`, `rollback` — then add CLI-level E2E-017–019.

- [ ] PostgreSQL Testcontainers profile for JDBC checkpoint E2E (`@Tag("jdbc")`).

- [ ] Parallel E2E (`junit.jupiter.execution.parallel.enabled`) after isolation verified.

---

## Quick verification commands

```bash
# Full unit + integration (existing)
./gradlew build

# E2E only (after R0.2)
./gradlew e2e

# Single scenario
./gradlew e2e --tests "io.sedna.tests.e2e.DnaRoundtripE2eTest"

# Benchmarks (non-blocking)
./gradlew jmh
```

---

## Progress summary

| Release | Scenarios | Status |
|---------|-----------|--------|
| R0 Harness | — | Done |
| R1 DNA/val | 001–005, 012–013 | Done (`io.sedna.tests.e2e`) |
| R2 Pipelines | 006–011, chain | Done; E2E-011 `@Disabled` |
| R3 Runtime/mutation | 014–019, 019B, 029–030 | Done |
| R4 Training/CLI | 020–025 | Done |
| R5 Bench/platform | 026–031 | Done; JMH documented in `benchmarks/README.md` |

**Remaining (optional):** sync `docs/sedna_e2e_test_plan.md` paths; migrate legacy tests to `@Tag("e2e")`; `--clean` CLI; JDBC profile; `verify-thresholds.sh`.
