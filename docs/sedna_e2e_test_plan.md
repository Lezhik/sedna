# SEDNA End-to-End Test Plan

| Field | Value |
|-------|-------|
| **Document version** | 1.0 |
| **Status** | Active |
| **Last updated** | 2026-Q2 |

---

# 1. Purpose

### Test Isolation Rule
Every E2E test must execute in a clean and isolated filesystem environment.

Requirements:
- Each test uses a dedicated temporary output directory.
- Previous test artifacts must never be reused.
- All CLI commands generating artifacts must support `--clean`.
- Parallel E2E execution must not share mutable filesystem state.
- Determinism validation must compare only freshly generated artifacts.

Isolation directories use the pattern:

```text
build/test-outputs/<test-id>/
```


This document defines the end-to-end (E2E) test scenarios for the SEDNA platform.

The goal of these tests is to validate:

- deterministic semantic processing
- DNA encoding and decoding
- forward pipeline generation
- reverse pipeline reconstruction
- runtime execution
- mutation safety
- replay reproducibility
- training pipeline reproducibility
- registry consistency
- rollback semantics

All tests are executed through CLI commands.

---

# 2. Test Environment

## Hardware Baseline

- 4 CPU cores
- 16 GB RAM
- SSD storage

---

## Software Baseline

| Component | Version |
|---|---|
| Java | 21 |
| Gradle | 8.x |
| PostgreSQL | 16 |
| OS | Linux/macOS |

---

## Test Dataset

Reference projects:

- examples/cms-reference
- examples/blog-reference
- examples/shop-reference

Reference DNA:

- examples/dna/cms-reference.sdna
- examples/dna/blog-reference.sdna

---

# 3. DNA Core Tests

## E2E-001 — DNA Encode

### CLI

```bash
./gradlew :sedna-dna:run --args="encode --input=examples/cms-reference.graph --output=build/cms.sdna"
```

### What is verified

- semantic graph encoding
- canonical TLV serialization
- deterministic binary generation

### Input

- SemanticGraph JSON
- registry definitions

### Expected Output

- cms.sdna binary file
- deterministic SHA-256 checksum
- no validation errors

---

## E2E-002 — DNA Decode

### CLI

```bash
./gradlew :sedna-dna:run --args="decode --input=examples/dna/cms-reference.sdna --output=build/cms.graph.json"
```

### What is verified

- DNA decoding
- registry resolution
- motif expansion
- graph reconstruction

### Input

- .sdna binary
- semantic registry

### Expected Output

- SemanticGraph JSON
- identical node count
- valid dependency topology

---

## E2E-003 — DNA Roundtrip Determinism

### CLI

```bash
./gradlew :sedna-tests:e2e --tests="DNARoundtripTest"
```

### What is verified

- encode(decode(x)) == x
- deterministic serialization
- stable node ordering

### Input

- cms-reference.sdna

### Expected Output

- byte-identical DNA output
- identical SHA-256 hashes

---

# 4. Registry Tests

## E2E-004 — Registry Bootstrap

### CLI

```bash
./gradlew :sedna-registry:run --args="bootstrap"
```

### What is verified

- embedded vocabulary loading
- registry initialization
- dependency resolution

### Input

- embedded core registry

### Expected Output

- initialized registry
- no unresolved references

---

## E2E-005 — Registry Conflict Validation

### CLI

```bash
./gradlew :sedna-registry:run --args="validate-conflicts --input=tests/conflicts"
```

### What is verified

- semantic conflict detection
- version conflict handling
- deterministic conflict resolution

### Input

- conflicting registry entries

### Expected Output

- conflict report
- no silent overwrite
- deterministic resolution order

---

# 5. Forward Pipeline Tests

## E2E-006 — Generate Spring Boot Project

### CLI

```bash
./gradlew :sedna-forward:run --args="generate --input=examples/dna/cms-reference.sdna --output=build/generated-cms"
```

### What is verified

- semantic graph expansion
- Java code generation
- Spring Boot reconstruction
- dependency generation

### Input

- cms-reference.sdna

### Expected Output

- compilable Spring Boot project
- generated Gradle build
- deterministic package structure

---

## E2E-007 — Generated Project Compilation

### CLI

```bash
cd build/generated-cms && ./gradlew build
```

### What is verified

- generated project validity
- dependency correctness
- generated code compilation

### Input

- generated project

### Expected Output

- BUILD SUCCESSFUL
- no compilation errors

---

## E2E-008 — Forward Determinism

### CLI

```bash
./gradlew :sedna-tests:e2e --tests="ForwardDeterminismTest"
```

### What is verified

- identical code generation across runs
- deterministic file ordering
- stable imports

### Input

- identical DNA input

### Expected Output

- byte-identical generated files
- identical project checksums

---

# 6. Reverse Pipeline Tests

## E2E-009 — Reverse Analysis

### CLI

```bash
./gradlew :sedna-reverse:run --args="analyze --input=examples/cms-reference --output=build/reversed.sdna"
```

### What is verified

- AST parsing
- semantic extraction
- graph reconstruction
- DNA generation

### Input

- Spring Boot CMS project

### Expected Output

- reversed.sdna
- semantic graph reconstruction
- no unresolved contracts

---

## E2E-010 — Reverse/Forward Equivalence

### CLI

```bash
./gradlew :sedna-tests:e2e --tests="ReverseForwardEquivalenceTest"
```

### What is verified

- reverse(forward(x)) consistency
- semantic equivalence
- deterministic node reconstruction

### Input

- cms-reference.sdna

### Expected Output

- identical NodeID set
- identical contract set
- identical constraint set

---

## E2E-011 — Git Trajectory Extraction

### CLI

```bash
./gradlew :sedna-reverse:run --args="extract-trajectories --input=examples/cms-reference-git"
```

### What is verified

- Git commit traversal
- semantic delta extraction
- deterministic trajectory ordering

### Input

- Git repository

### Expected Output

- trajectory dataset
- stable ordering
- deterministic semantic deltas

---

# 7. Validation Tests

## E2E-012 — Semantic Validation

### CLI

```bash
./gradlew :sedna-validation:run --args="validate --input=examples/dna/cms-reference.sdna"
```

### What is verified

- semantic constraints
- contract validation
- dependency validation

### Input

- DNA file

### Expected Output

- validation success
- no unresolved dependencies

---

## E2E-013 — Invalid Graph Rejection

### CLI

```bash
./gradlew :sedna-validation:run --args="validate --input=tests/invalid/invalid-graph.sdna"
```

### What is verified

- invalid graph detection
- constraint violation handling
- deterministic validation order

### Input

- intentionally invalid DNA

### Expected Output

- validation failure
- deterministic error report

---

# 8. Runtime Engine Tests

## E2E-014 — DAG Runtime Execution

### CLI

```bash
./gradlew :sedna-runtime:run --args="execute --input=examples/dna/cms-reference.sdna"
```

### What is verified

- DAG scheduling
- execution ordering
- dependency handling

### Input

- valid DNA graph

### Expected Output

- successful execution
- canonical execution order
- no runtime validation failures

---

## E2E-015 — Runtime Replay

### CLI

```bash
./gradlew :sedna-runtime:run --args="replay --execution-id=test-001"
```

### What is verified

- deterministic replay
- replay consistency
- state reconstruction

### Input

- execution snapshot
- replay metadata

### Expected Output

- replay identical to original execution
- identical execution graph

---

## E2E-016 — Runtime Checkpoint Recovery

### CLI

```bash
./gradlew :sedna-runtime:run --args="recover --checkpoint=checkpoint-001"
```

### What is verified

- checkpoint restoration
- replay continuation
- deterministic recovery

### Input

- runtime checkpoint

### Expected Output

- successful recovery
- restored execution state

---

# 9. Mutation Engine Tests

## E2E-017 — Subtree Mutation

### CLI

```bash
./gradlew :sedna-mutation:run --args="mutate --input=examples/dna/cms-reference.sdna --mutation=tests/mutations/add-payment-module.json"
```

### What is verified

- subtree replacement
- semantic rewrite safety
- dependency preservation

### Input

- DNA graph
- mutation definition

### Expected Output

- valid mutated DNA
- preserved semantic topology

---

## E2E-018 — Mutation Rollback

### CLI

```bash
./gradlew :sedna-mutation:run --args="rollback --snapshot=mutation-001"
```

### What is verified

- rollback semantics
- snapshot restoration
- deterministic state recovery

### Input

- mutation snapshot

### Expected Output

- restored pre-mutation graph
- no semantic corruption

---

## E2E-019 — Invalid Mutation Rejection

### E2E-019B — Deep Mutation Drift Longevity
* **CLI:**
  ```bash
  ./gradlew :sedna-tests:e2e --tests="com.sedna.tests.e2e.DeepMutationDriftTest"
  ```

* **What is verified:** Long-term mutation stability across repeated evolutionary steps.
* **Input:** Base DNA + sequence of 10 valid constraint-preserving mutations.
* **Expected Output:**
  - Final DNA remains structurally valid.
  - Generated project compiles successfully.
  - No uncontrolled graph bloat occurs.
  - Dependency topology remains deterministic.
  - Runtime validation passes after all generations.

E2E-019 — Invalid Mutation Rejection

### CLI

```bash
./gradlew :sedna-mutation:run --args="mutate --input=tests/invalid/broken.sdna --mutation=tests/mutations/invalid-cross-domain.json"
```

### What is verified

- forbidden cross-domain rewrite rejection
- semantic validation integration

### Input

- invalid mutation

### Expected Output

- mutation rejected
- validation error report

---

# 10. Training Pipeline Tests

## E2E-020 — Training Dataset Generation

### CLI

```bash
./gradlew :sedna-training:run --args="generate-dataset --input=datasets/cms-projects"
```

### What is verified

- project ingestion
- semantic extraction
- dataset generation

### Input

- multiple CMS repositories

### Expected Output

- training dataset
- deterministic dataset ordering

---

## E2E-021 — Motif Discovery

### CLI

```bash
./gradlew :sedna-training:run --args="discover-motifs --input=datasets/cms-projects"
```

### What is verified

- repeated semantic structure discovery
- motif extraction
- deterministic motif IDs

### Input

- training dataset

### Expected Output

- motif registry
- stable motif hashes

---

## E2E-022 — Embedding Generation

### Cross-Platform Floating Point Rule
Embedding validation must tolerate hardware-specific floating-point optimizations.

Validation rules:
- Cosine similarity must be `>= 0.9999` across repeated executions.
- Absolute float delta tolerance: `epsilon <= 1e-6`.
- Strict binary hash equality is forbidden for floating-point vector outputs.
- Embedding topology and nearest-neighbor relationships must remain stable.

E2E-022 — Embedding Generation

### CLI

```bash
./gradlew :sedna-training:run --args="generate-embeddings --input=datasets/cms-projects"
```

### What is verified

- deterministic semantic embeddings
- embedding reproducibility
- vector indexing

### Input

- semantic trajectories

### Expected Output

- embedding dataset
- deterministic vector output

---

## E2E-023 — Registry Learning

### CLI

```bash
./gradlew :sedna-training:run --args="update-registry --input=datasets/cms-projects"
```

### What is verified

- registry extension generation
- semantic vocabulary learning
- conflict handling

### Input

- training trajectories

### Expected Output

- updated semantic registry
- deterministic registry ordering

---

# 11. CLI Tests

## E2E-024 — CLI Help

### CLI

```bash
./gradlew :sedna-cli:run --args="help"
```

### What is verified

- CLI initialization
- command discovery
- help rendering

### Input

- none

### Expected Output

- command list
- usage information

---

## E2E-025 — Invalid CLI Arguments

### CLI

```bash
./gradlew :sedna-cli:run --args="generate --unknown-option"
```

### What is verified

- invalid argument handling
- deterministic error reporting

### Input

- invalid CLI arguments

### Expected Output

- stable error message
- non-zero exit code

---

# 12. Performance Tests

## E2E-026 — DNA Decode Benchmark

### CLI

```bash
./gradlew :sedna-benchmarks:run --args="decode-benchmark"
```

### What is verified

- DNA decode performance
- deterministic benchmark execution

### Input

- reference DNA dataset

### Expected Output

- decode time <100ms

---

## E2E-027 — Forward Reconstruction Benchmark

### CLI

```bash
./gradlew :sedna-benchmarks:run --args="forward-benchmark"
```

### What is verified

- project generation performance

### Input

- reference DNA

### Expected Output

- generation time <5s

---

## E2E-028 — Reverse Analysis Benchmark

### CLI

```bash
./gradlew :sedna-benchmarks:run --args="reverse-benchmark"
```

### What is verified

- reverse pipeline performance

### Input

- cms-reference project

### Expected Output

- reverse analysis <30s

---

# 13. Failure and Recovery Tests

## E2E-029 — Registry Corruption Recovery

### CLI

```bash
./gradlew :sedna-tests:e2e --tests="RegistryRecoveryTest"
```

### What is verified

- corrupted registry handling
- fallback recovery
- deterministic bootstrap restoration

### Input

- corrupted registry snapshot

### Expected Output

- recovered registry state
- no silent corruption

---

## E2E-030 — Interrupted Runtime Recovery

### CLI

```bash
./gradlew :sedna-tests:e2e --tests="InterruptedRuntimeRecoveryTest"
```

### What is verified

- interrupted execution recovery
- checkpoint restoration
- replay consistency

### Input

- interrupted runtime execution

### Expected Output

- successful continuation
- deterministic final state

---

# 14. Determinism Validation Suite

## E2E-031 — Full Platform Determinism

### CLI

```bash
./gradlew :sedna-tests:e2e --tests="FullDeterminismSuite"
```

### What is verified

- identical outputs across repeated runs
- registry reproducibility
- replay consistency
- stable graph generation

### Input

- reference projects
- reference DNA
- reference registry

### Expected Output

- 100% deterministic equivalence
- identical hashes across runs

---

# 15. Validation Chain Strategy

Recommended deterministic validation chain:

```text
E2E-001 Encode
→ E2E-002 Decode
→ E2E-006 Forward Generate
→ E2E-007 Generated Project Compile
→ E2E-009 Reverse Analysis
→ E2E-010 Reverse/Forward Equivalence
```

Validation objective:
- Ensure semantic preservation across the entire platform lifecycle.
- Detect cross-module incompatibilities.
- Validate mathematical reversibility constraints.
- Prevent hidden pipeline drift between stages.

A successful validation chain demonstrates compiler-grade semantic consistency.

---

Final Validation Rule

A test is considered successful only if:

- outputs are deterministic
- semantic equivalence is preserved
- no hidden runtime side effects exist
- replay results are reproducible
- canonical ordering is preserved

Non-deterministic behavior is considered a platform failure.

