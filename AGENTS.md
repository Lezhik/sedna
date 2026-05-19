# AGENTS.md

## SEDNA AI-Agent Operational Rules

This document defines mandatory operational rules for AI agents contributing to SEDNA.

All agents must follow these rules exactly.

Violation of determinism, canonical DTO usage, or semantic consistency is considered a critical implementation defect.

---

# 1. Core Rules

## Rule 1 — Determinism First

If multiple valid implementations exist, always choose the deterministic one.

Forbidden:
- random ordering
- unordered collection iteration
- runtime-generated identifiers
- scheduler-dependent execution order
- non-repeatable serialization

Required:
- canonical ordering
- stable identifiers
- repeatable outputs
- deterministic graph traversal
- deterministic replay

---

## Rule 2 — Canonical Definitions Only

Do not redefine:
- DTOs
- semantic models
- NodeID logic
- contract structures
- graph structures
- registry references

All shared models must be imported from `sedna-core`.

---

# 2. Repository Structure

```text
sedna-core/           Canonical DTOs and utilities
sedna-dna/            DNA encoding and decoding
sedna-registry/       Semantic registry
sedna-forward/        Forward pipeline
sedna-reverse/        Reverse pipeline
sedna-training/       Training pipeline
sedna-runtime/        Runtime engine
sedna-mutation/       Mutation engine
sedna-validation/     Validation engine
sedna-persistence/    Persistence layer
sedna-cli/            CLI tooling
examples/             Reference projects
tests/                Shared integration tests
docs/                 Specifications
```

---

# 3. Canonical DTO Rules

All modules must import DTOs from `sedna-core`.

Forbidden:
- duplicate DTO definitions
- local graph models
- alternative NodeID implementations
- module-specific graph structures

Canonical DTOs:
- SemanticGraph
- GenomeNode
- SemanticLink
- Contract
- Constraint
- ExecutionToken
- Mutation
- MutationResult
- SemanticError

---

# 4. SemanticGraph Definition

```java
record SemanticGraph(
    List<GenomeNode> nodes,
    List<SemanticLink> links,
    RegistryVersion vocabularyVersion
) {}

record GenomeNode(
    long nodeId,
    NodeKind kind,
    SemanticCore core,
    List<Contract> contracts,
    List<Constraint> constraints
) {}

record SemanticLink(
    long sourceNodeId,
    long targetNodeId,
    LinkType type
) {}
```

All agents must use this structure exactly.

---

# 5. Deterministic Collection Rules

Forbidden:
- HashMap iteration
- HashSet iteration
- non-stable stream ordering

Allowed:
- LinkedHashMap
- TreeMap
- ImmutableList
- ImmutableMap

Canonical ordering:

```java
CanonicalOrdering.comparator()
```

Tie-breaker:

```text
lexicographic NodeID ordering
```

---

# 6. Bootstrap Order

Mandatory initialization sequence:

```text
1. Load embedded core vocabulary
2. Initialize minimal DNA decoder
3. Decode registry extensions
4. Initialize semantic registry
5. Initialize validation engine
6. Initialize pipelines
7. Initialize runtime engine
```

Do not reorder these steps.

This sequence prevents circular dependency between:
- DNA decoder
- semantic registry
- registry extensions

---

# 7. Error Handling Rules

Public module boundaries must never expose raw exceptions.

Required boundary type:

```java
Result<T, SemanticError>
```

SemanticError structure:

```java
record SemanticError(
    ErrorCode code,
    long nodeId,
    String message
)
```

Forbidden:
- RuntimeException across module boundaries
- nullable error responses
- string-only errors

---

# 8. Mutation Rules

## 8.1 Allowed Mutation Scope

Subtree scope:

```text
parent node
+ all CHILDREN recursively
+ local LINKS only
```

Cross-domain graph rewrites:

```text
FORBIDDEN
```

---

## 8.2 Mutation Constraints

Mutation must preserve:
- graph validity
- contract consistency
- constraint consistency
- deterministic NodeIDs

Forbidden:
- orphan nodes
- broken contracts
- unresolved capabilities
- non-deterministic rewrites

---

# 9. Runtime Rules

## 9.1 Runtime Determinism

Runtime ordering must use:
1. dependency topology
2. canonical NodeID tie-breaker

Project Reactor schedulers must preserve canonical execution ordering.

Parallel branches must use dependency-aware sequencing.

---

# 10. Replay Rules

Replay must reproduce:
- semantic execution order
- state transitions
- NodeID traversal
- contract resolution order

Replay must NOT reproduce:
- timestamps
- random values
- external HTTP payloads
- non-semantic metadata

---

# 11. LLM Rules

## Allowed

LLM usage allowed only for:
- UNKNOWN semantic enrichment
- semantic classification
- documentation synthesis
- semantic suggestion generation

---

## Forbidden

LLM must NEVER:
- generate final DNA
- assign NodeIDs
- define contracts
- define constraints
- mutate semantic graphs directly
- bypass validation

---

## LLM Failure Fallback

Mandatory fallback:

```text
empty method body skeleton
```

Pipeline execution must continue.

---

# 12. Validation Rules

## 12.1 Required Validation

All pipelines must validate:
- graph topology
- contracts
- constraints
- motif references
- capability resolution
- deterministic ordering

---

## 12.2 Semantic Equivalence

Semantic equivalence requires:
- identical NodeID set
- identical node count
- identical contract set
- identical constraint set
- equivalent motif expansion
- equivalent dependency topology

---

# 13. Performance Targets

Baseline:

```text
4-core CPU
16GB RAM
SSD storage
warmed JVM
```

Targets:

| Operation | Target |
|---|---|
| DNA decode | <100ms |
| DNA encode | <100ms |
| Forward reconstruction | <5s |
| Reverse analysis | <30s |
| Registry lookup | <5ms |

Reference project:

```text
examples/cms-reference
```

---

# 14. Interface Evolution Policy

Stabilization point:

```text
Phase 2 acceptance criteria passed
```

After stabilization:
- additive API changes allowed
- breaking API changes forbidden

Required:
- semantic version increment
- validation rerun
- compatibility verification

---

# 15. Module Versioning

Each module must publish semantic version in:

```text
gradle.properties
```

Versioning rules:
- additive API change → minor version increment
- breaking change → major version increment

---

# 16. Documentation Rules

Every public module must contain:
- README.md
- architecture notes
- deterministic behavior description
- rollback semantics
- failure semantics
- benchmark results
- public API examples

Minimum requirement:

```text
one working code example per public interface
```

---

# 17. Forbidden Engineering Patterns

Forbidden:
- hidden global state
- reflection-heavy runtime logic
- runtime bytecode mutation
- mutable shared registries
- unordered graph traversal
- silent retries
- implicit dependency injection
- non-versioned contracts
- cross-module DTO duplication

---

# 18. Development Workflow

Required implementation order:

```text
1. sedna-core
2. sedna-dna
3. sedna-registry
4. sedna-validation
5. sedna-forward
6. sedna-reverse
7. sedna-mutation
8. sedna-runtime
9. sedna-training
10. sedna-cli
```

Parallel implementation rule:

```text
sedna-forward and sedna-reverse may be implemented in parallel
after sedna-core, sedna-dna, sedna-registry,
and sedna-validation are complete.
```

Do not skip dependency order.

---

# 19. Final Rule

Non-deterministic semantic behavior is always a bug.

If implementation simplicity conflicts with determinism:
choose determinism.

