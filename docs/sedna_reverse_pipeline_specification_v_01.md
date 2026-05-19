# SEDNA Reverse Pipeline Specification v1

## 1. Purpose

This document defines the **Reverse Pipeline** of SEDNA.
The pipeline transforms an existing Java Spring Boot codebase into compact SEDNA DNA.

The reverse process reconstructs:

> Source Code → Semantic Graph → Semantic Motifs → Compact DNA

The pipeline is deterministic whenever possible.
LLM usage is optional and restricted to semantic enrichment only.

---

# 2. Input / Output Definition

## Input

- Java Spring Boot project
- Gradle build files
- Git repository history (optional but recommended)
- Vocabulary Registry
- Contract Registry

## Output

- Compact SEDNA DNA (binary TLV)
- Semantic Hypergraph
- Semantic Motif Library
- Contract Definitions

---

# 3. Step 1 — Source Parsing

## Objective

Parse Java source code into structural AST and bytecode models.

## Input

- Java source files
- Compiled bytecode (optional)

## Output

- Parsed AST
- Bytecode metadata
- Symbol tables

## Algorithms / Technologies

| Tool / Algorithm | Usage |
|---|---|
| Spoon | Java AST parsing |
| ASM | Bytecode inspection |
| JavaParser | Lightweight syntax parsing |
| Gradle Tooling API | Build structure extraction |

## Example

### Input

```java
@RestController
class UserController {
    private final UserService service;
}
```

### Output

```text
CLASS: UserController
ANNOTATION: RestController
DEPENDENCY: UserService
```

---

# 4. Step 2 — Structural Graph Construction

## Objective

Build structural dependency graph from parsed code.

## Input

- AST
- Bytecode metadata
- Symbol tables

## Output

- Structural Graph

## Algorithms / Technologies

| Tool / Algorithm | Usage |
|---|---|
| Directed Graph Builder | Dependency graph construction |
| Tarjan SCC | Cycle detection |
| Dependency extraction | Service wiring analysis |

## Graph Elements

| Node Type | Example |
|---|---|
| Class | UserController |
| Service | UserService |
| Repository | UserRepository |
| Configuration | SecurityConfig |

## Example

```text
UserController -> UserService
UserService -> UserRepository
```

---

# 5. Step 3 — Semantic Extraction

## Objective

Transform structural graph into semantic graph.

## Input

- Structural Graph
- Vocabulary Registry

## Output

- Typed Semantic Hypergraph

## Algorithms / Technologies

| Tool / Algorithm | Usage |
|---|---|
| Vocabulary matching | Semantic classification |
| Annotation analysis | Spring semantic extraction |
| Semantic role inference | Domain classification |
| Contract inference | Capability reconstruction |

## Unmatched Structural Elements

Unmatched structural elements are:

- classified as UNKNOWN semantic nodes
- flagged for optional LLM enrichment (Step 11)
- serialized with UNKNOWN semantic type if enrichment fails

## Semantic Mapping Example

### Structural Pattern

```text
@RestController
@Service
@Repository
```

### Semantic Graph

```text
DOMAIN.API.CONTROLLER
DOMAIN.SERVICE.APPLICATION
DOMAIN.PERSISTENCE.REPOSITORY
```

---

# 6. Step 4 — Capability and Contract Reconstruction

## Objective

Infer semantic contracts and capabilities.

## Input

- Semantic Hypergraph

## Output

- Capability Contracts
- Dependency Contracts

## Algorithms / Technologies

| Tool / Algorithm | Usage |
|---|---|
| Dependency graph analysis | Service contract extraction |
| Spring Bean analysis | DI capability discovery |
| Interface matching | Capability grouping |

## Example

### Source

```java
class UserService implements IUserService
```

### Contract

```text
Provides: USER_SERVICE@1.0
Requires: USER_REPOSITORY@1.0
```

## Contract Version Inference

- explicit: inferred from @Version annotation or interface version marker
- implicit: v1.0 assigned to reconstructed contracts without version markers

---

# 7. Step 5 — Motif Detection and Folding

## Objective

Compress semantic graph into reusable motifs.

## Input

- Semantic Hypergraph
- Motif Registry

## Output

- Folded Semantic Graph
- Motif references

## Algorithms / Technologies

| Tool / Algorithm | Usage |
|---|---|
| Graph signature matching | Pattern identification |
| Structural similarity | Approximate motif detection |
| Contract compatibility | Motif validation |
| Semantic role matching | Behavioral grouping |

## Folding Rule

Subgraphs matching registered semantic motifs are replaced with motif references and semantic overrides.

Folding policy:
- exact match → fold unconditionally
- approximate match → fold with PARTIAL_MATCH flag
- similarity threshold defined in SEDNA-FOLD-v1

## Example

### Before Folding

```text
Controller -> Service -> Repository
```

### After Folding

```text
CRUD_STACK_MOTIF
```

## External Specification

Folding algorithm details:
- SEDNA-FOLD-v1

---

# 8. Step 6 — Context and Scope Reconstruction

## Objective

Rebuild semantic execution boundaries.

## Input

- Folded Semantic Graph

## Output

- Context-aware Semantic Graph

## Algorithms / Technologies

| Tool / Algorithm | Usage |
|---|---|
| Package analysis | Context grouping |
| Dependency clustering | Module detection |
| Transaction boundary analysis | Stateful region detection |

## Context Types

| Context | Example |
|---|---|
| LOCAL | Utility class |
| MODULE | User domain module |
| DOMAIN | Business bounded context |
| ISOLATED | Security subsystem |

---

# 9. Step 7 — Genome Serialization

## Objective

Serialize semantic graph into compact DNA.

## Input

- Context-aware Semantic Graph

## Output

- Compact DNA binary stream

## Algorithms / Technologies

| Tool / Algorithm | Usage |
|---|---|
| TLV serializer | Binary encoding |
| SHA-256 hashing | Stable NodeID generation |
| Canonical ordering | Deterministic serialization |

## Serialization Rules

- Little-endian binary encoding
- Deterministic field ordering
- Canonical child ordering
- Stable vocabulary references

## Example

```text
[HEADER]
[SEMANTIC_CORE]
[CONTRACTS]
[LINKS]
[CONSTRAINTS]
[CHILDREN]
```

---

# 10. Git-Aware Semantic Reconstruction

## Objective

Extract semantic evolution history from Git commits.

## Input

- Git repository

## Output

- Semantic trajectory history
- Incremental genome evolution

## Algorithms / Technologies

| Tool / Algorithm | Usage |
|---|---|
| Git diff analysis | Structural change extraction |
| AST differencing | Semantic delta extraction |
| Commit graph traversal | Trajectory reconstruction |

## Important Rule

Single Git commits may contain multiple semantic deltas.
The analyzer must split commits into atomic semantic transitions.

Atomic semantic transition definition:
- minimal graph delta
- changes exactly one semantic node or one contract
- split boundary is semantic-node-based, not file-based

## Example

### Git Commit

```text
Added User entity, controller and tests
```

### Extracted Deltas

```text
ADD_ENTITY(User)
ADD_CONTROLLER(UserController)
ADD_TEST(UserControllerTest)
```

---

# 11. Optional LLM Semantic Enrichment

## Objective

Improve semantic labeling and motif recognition.

## Input

- Semantic Graph
- Structural motifs

## Output

- Enriched semantic metadata

## Rules

LLM is NOT allowed to:

- modify graph structure
- create contracts
- create dependencies
- generate DNA directly

LLM may ONLY:

- improve semantic labels
- suggest motif candidates
- classify ambiguous structures

## Failure Handling

LLM failure does NOT stop pipeline execution.
Pipeline falls back to deterministic semantic extraction.

---

# 12. Determinism Rules

## Deterministic Stages

- source parsing
- graph construction
- contract reconstruction
- serialization
- hashing

## Semi-Deterministic Stages

- motif approximation
- semantic enrichment

---

# 13. Validation Pipeline

## Validation Order

Constraint propagation follows contract reconstruction intentionally.
Constraints may invalidate reconstructed contracts and trigger rollback/re-resolution using SEDNA v1 transaction semantics.

1. Structural coherence
2. Contract reconstruction
3. Constraint propagation
4. Motif validation
5. Serialization consistency

## Rollback Rule

Validation failure triggers rollback using SEDNA v1 mutation transaction semantics.

---

# 14. MVP Constraints

- Spring Boot monolith only
- Single Gradle project
- No distributed tracing reconstruction
- No runtime behavior inference
- No production telemetry analysis

---

