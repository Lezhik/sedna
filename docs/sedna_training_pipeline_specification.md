# SEDNA Training Pipeline Specification

| Field | Value |
|-------|-------|
| **Document version** | 1.0 |
| **Status** | Active |
| **Last updated** | 2026-Q2 |

---

## 1. Purpose

The SEDNA Training Pipeline converts existing Spring Boot REST CMS projects into deterministic semantic datasets used for:

- vocabulary growth
- motif discovery
- mutation scoring
- semantic compression
- trajectory learning

The output is not executable code.
The output is compact semantic DNA and registry updates.

---

# 2. Scope

## Included

- Spring Boot monoliths
- REST APIs
- CRUD services
- JPA/Hibernate
- Security/filter chains
- Validation layers
- Transaction boundaries

## Out of Scope (Current Version)

The training pipeline currently targets **Spring Boot monoliths** only. The following domains are **not supported in the current release** and are deferred until core Java/Spring functionality is complete:

- distributed systems architectures
- Kafka / message-bus-centric designs
- microservices decomposition
- Kubernetes / cloud-native deployment patterns
- distributed sagas
- reactive pipelines

These may be added in a future release after the primary pipeline is stabilized; they are not current-version deliverables.

---

# 3. Pipeline Overview

| Step | Stage |
|---|---|
| 1 | Repository Loading |
| 2 | Structural Parsing |
| 3 | Semantic Extraction |
| 4 | Contract Reconstruction |
| 5 | Context Reconstruction |
| 6 | Hypergraph Construction |
| 7 | Semantic Delta Extraction |
| 8 | Trajectory Construction |
| 9 | Genome Compression |
| 10 | Mutation Dataset Generation |
| 11 | Motif Folding |
| 12 | Deterministic Ordering |
| 13 | Validation |
| 14 | Embedding Generation |
| 15 | LLM Enrichment |
| 16 | Registry Updates |
| 17 | Dataset Requirements |
| 18 | Coverage Goals |
| 19 | External Technologies |
| 20 | Determinism Rules |

---

# 4. Repository Loading

## 4.1 Rule

One repository may contain many projects.
Training analysis must operate on project folders, never on the repository as a whole.

## 4.2 Forbidden

Forbidden:

```text
Repository-wide semantic reconstruction
```

Allowed:

```text
/repository/project-a
/repository/project-b
/repository/project-c
```

## 4.3 Technologies

| Purpose | Technology |
|---|---|
| Git history | Git |
| Checkout | JGit |
| File traversal | Java NIO |

---

# 5. Structural Parsing

## Input

Java source tree.

## Output

Deterministic structural graph.

## Algorithms

| Purpose | Algorithm |
|---|---|
| AST parsing | Spoon |
| Bytecode inspection | ASM |
| Dependency extraction | Graph traversal |

---

# 6. Semantic Extraction

## Input

Structural graph.

## Output

Semantic graph.

## Rules

Vocabulary matching converts structural nodes into semantic nodes.

Example:

```text
@RestController
→ DOMAIN.API.CONTROLLER
```

## Unknown Elements

Unmatched structural elements:

- classified as UNKNOWN semantic node
- flagged for LLM enrichment
- serialized as UNKNOWN if enrichment fails

---

# 7. Contract Reconstruction

## Input

Semantic graph.

## Output

Capability contracts.

## Example

```text
Provides: USER_SERVICE@1.0
Requires: USER_REPOSITORY@>=1.0
```

## Version Inference

Explicit:

- @Version annotations
- interface markers

Implicit:

- default reconstructed version = v1.0

---

# 8. Context Reconstruction

| Context | Meaning |
|---|---|
| LOCAL | Single node scope |
| MODULE | Technical subsystem |
| DOMAIN | Business bounded context |
| ISOLATED | Sandboxed execution area |

Detail: SEDNA-CONTEXT-v1

---

# 9. Hypergraph Construction

## Input

Semantic nodes + contracts + contexts.

## Output

Typed semantic hypergraph.

## Algorithms

| Purpose | Algorithm |
|---|---|
| Link creation | Capability resolution |
| Graph assembly | Typed hypergraph construction |
| Validation | DAG cycle detection |

---

# 10. Semantic Delta Extraction

## Purpose

Extract semantic differences between graph snapshots.

## Atomic Rule

Atomic semantic transition:

```text
minimal graph delta
changing exactly one semantic node
or one contract
```

Split boundary:

```text
semantic node boundary
NOT file boundary
```

## Algorithms

| Purpose | Algorithm |
|---|---|
| Snapshot comparison | Approximated graph edit distance |
| Delta extraction | Graph differencing |

Exact graph edit distance intentionally avoided.
Detail: SEDNA-FOLD-v1

---

# 11. Motif Folding

## Purpose

Compress repeated semantic structures into reusable motifs.

## Folding Policy

Exact match:

```text
fold unconditionally
```

Approximate match:

```text
similarity >= threshold
→ fold with PARTIAL_MATCH flag
```

Threshold detail:

```text
SEDNA-FOLD-v1
```

## Algorithms

| Purpose | Algorithm |
|---|---|
| Structural matching | Graph signatures |
| Similarity | Structural similarity |
| Validation | Contract compatibility |

---

# 12. Trajectory Construction

## Purpose

Build deterministic semantic evolution trajectories.

## Ordering Rules

Primary ordering:

- Git commit ordering
- dependency topology

Parallel deltas inside one commit:

```text
1. dependency graph topology
2. canonical node ID (lexicographic)
```

---

# 13. Genome Compression

## Purpose

Convert semantic graphs into compact DNA.

## Techniques

| Purpose | Technique |
|---|---|
| Vocabulary compression | Dictionary encoding |
| Motif reuse | Registry references |
| Binary serialization | TLV encoding |

Example fragment:

```text
A1F2-CC19-0FA1
```

Illustrative only.
Not full TLV format.

---

# 14. Embedding Generation

## 14.1 Purpose

Generate deterministic semantic embeddings.

## 14.2 Embedding Target

Embedding target:

```text
semantic vocabulary path
+ contract signature
```

## 14.3 Rules

Embedding model:

```text
deterministic semantic encoder
```

No runtime LLM dependency allowed.

Detail:

```text
SEDNA-EMBED-v1
```

## 14.4 Algorithms

| Purpose | Algorithm |
|---|---|
| Semantic indexing | Embedding index |
| Similarity clustering | Vector similarity search |

---

# 15. LLM Enrichment

## Purpose

Optional enrichment for UNKNOWN semantic nodes.

## Allowed

- naming suggestions
- semantic hints
- weak classification

## Forbidden

- registry mutation
- deterministic ordering
- final DNA generation
- contract generation

## Failure Handling

If enrichment fails:

```text
fallback = UNKNOWN semantic node
```

---

# 16. Registry Updates

## 16.1 Purpose

Update semantic registries using validated trajectories.

## 16.2 Conflict Resolution

Exact match:

```text
skip update
```

Version conflict:

```text
append new version
retain previous versions
```

Semantic conflict:

```text
flag for manual review
never overwrite automatically
```

## Updated Registries

- vocabulary registry
- motif registry
- contract registry
- embedding registry
- mutation scoring registry

---

# 17. Dataset Requirements

## 17.1 Minimum Dataset

Minimum viable dataset:

```text
20-30 Spring Boot CMS projects
```

Expected output:

```text
2k-5k semantic nodes
100-300 motifs
```

## 17.2 Recommended Dataset Size

Recommended dataset:

```text
80-150 projects
```

Expected output:

```text
10k-30k semantic nodes
500-1500 motifs
```

Estimates based on average Spring Boot CMS projects containing:

- 30-100 entities
- 10-40 service boundaries
- CRUD + security + validation layers

---

# 18. Coverage Goals

## 18.1 Goal

Coverage measures how often semantic patterns appear consistently across projects.

## 18.2 Scale

| Level | Meaning |
|---|---|
| High | >70% of projects |
| Medium | 40-70% |
| Low | <40% |

## 18.3 Target Coverage

| Domain | Coverage |
|---|---|
| CRUD controllers | High |
| JPA repositories | High |
| Validation flows | High |
| Security chains | Medium |
| Custom workflows | Medium |
| Complex orchestration | Low |

---

# 19. External Technologies

| Purpose | Technology |
|---|---|
| AST parsing | Spoon |
| Bytecode parsing | ASM |
| Build execution | Gradle |
| Repository access | Git / JGit |
| Graph storage | JGraphT |
| Validation | ArchUnit |
| LLM enrichment | Optional LLM |

---

# 20. Determinism Rules

Deterministic:

- graph ordering
- node IDs
- vocabulary encoding
- contract reconstruction
- motif folding rules
- registry updates

Semi-deterministic:

- UNKNOWN enrichment
- semantic naming hints

Non-deterministic outputs are forbidden inside final DNA.

---

# 21. Final Principle

The SEDNA Training Pipeline exists to learn deterministic semantic evolution patterns from real projects and convert them into compact reusable DNA suitable for forward generation, reverse reconstruction, and mutation-safe evolution.

