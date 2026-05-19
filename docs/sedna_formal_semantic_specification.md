# SEDNA Formal Semantic Specification

| Field | Value |
|-------|-------|
| **Document version** | 1.0 |
| **Status** | Active |
| **Last updated** | 2026-Q2 |

---

## 1. Scope

SEDNA defines a compact executable semantic genome for software systems.

SEDNA stores semantic intent and executable semantic structure instead of source code.

Target stack:
- Java
- Spring Boot
- Gradle

SEDNA defines:
- canonical genome grammar
- binary serialization
- typed semantic hypergraph structure
- contract system
- execution semantics
- mutation semantics
- semantic registry resolution
- reverse engineering rules

---

# 2. Architecture Layers

| Layer | Purpose |
|---|---|
| Compact DNA | Canonical binary genome |
| Semantic Hypergraph | Runtime semantic representation |
| Source Projection | Generated executable system |

Forward pipeline:

```text
Compact DNA
    ↓
Decoder
    ↓
Semantic Hypergraph
    ↓
Executor
    ↓
Spring Boot Project
```

Reverse pipeline:

```text
Spring Boot Project
    ↓
Analyzer
    ↓
Semantic Hypergraph
    ↓
Encoder
    ↓
Compact DNA
```

---

# 3. Canonical Genome Grammar

## 3.1 Genome Node

Every semantic executable unit is a Genome Node.

```ebnf
NODE :=
    HEADER
    SEMANTIC_CORE
    CONTRACTS
    LINKS
    CONSTRAINTS
    CONTEXT
    CHILDREN*
```

Nodes are recursively composable.

CHILDREN may contain nested NODE structures.

---

## 3.2 Binary Serialization

SEDNA is binary-first.

Human-readable representations are projections only.

Canonical encoding:
- Little Endian
- uint8 / uint16 / uint32 / uint64 primitives
- TLV (Type-Length-Value) variable sections
- UTF-8 strings
- length-prefixed arrays
- recursive child blocks

Canonical node framing:

```text
[NODE_HEADER]
[TLV:SEMANTIC_CORE]
[TLV:CONTRACTS]
[TLV:LINKS]
[TLV:CONSTRAINTS]
[TLV:CONTEXT]
[TLV:CHILDREN]
```

Node structure:

| Field | Type |
|---|---|
| NodeID | uint64 |
| NodeKind | uint16 |
| VocabularyVersion | uint16 |
| SemanticCore | variable |
| Contracts | variable |
| Links | variable |
| Constraints | variable |
| Context | variable |
| Children | variable |

NodeID generation:
- SHA-256 canonical node hash
- first 64 bits used as NodeID
- stable until semantic mutation occurs

Detailed binary framing is delegated to:
`SEDNA-BIN-v1`

---

# 4. HEADER

HEADER defines node interpretation.

## 4.1 Grammar

```ebnf
HEADER :=
    NODE_KIND
    EXECUTION_PROFILE
    COMPRESSION_LEVEL
    VOCABULARY_VERSION
    PROVENANCE
```

---

## 4.2 Node Kinds

| Node Kind | Purpose |
|---|---|
| ENTITY | Domain entity |
| SERVICE | Stateless semantic logic |
| WORKFLOW | Orchestration node |
| POLICY | Constraint provider |
| CONTROLLER | API boundary |
| INTEGRATION | External system integration |
| MOTIF | Folded semantic structure |

---

## 4.3 Execution Profiles

| Profile | Meaning |
|---|---|
| DAG | Deterministic dependency execution |
| SUPERVISOR | Side-effect orchestration |
| STATEFUL | Managed state transitions |

Execution profile mutation rules:
- DAG → DAG allowed
- DAG → STATEFUL requires structural validation
- STATEFUL → DAG forbidden unless cycles removed
- SUPERVISOR mutations require contract revalidation

---

# 5. SEMANTIC_CORE

## 5.1 Purpose

SEMANTIC_CORE defines semantic intent.

It stores compact references into the Semantic Registry.

SEMANTIC_CORE never stores implementation code.

---

## 5.2 Canonical Grammar

```ebnf
SEMANTIC_CORE :=
    CLASS_REF
    TARGET_REF
    OPERATION_REF
    MODIFIER_REF*

CLASS_REF := VOCAB ':' TERM_PATH
TARGET_REF := VOCAB ':' TERM_PATH
OPERATION_REF := VOCAB ':' TERM_PATH
MODIFIER_REF := VOCAB ':' TERM_PATH

VOCAB := [a-z][a-z0-9_]*
TERM_PATH := TERM ('.' TERM)*
TERM := [A-Z][A-Z0-9_]*
```

Example:

```text
core:DOMAIN.ENTITY.AGGREGATE
spring:SERVICE.STATELESS
core:CREATE.VALIDATED.TRANSACTIONAL
core:AUDITED
```

MODIFIER_REF ordering is deterministic.

Absence of modifiers is valid.

---

## 5.3 Vocabulary Resolution

Vocabulary resolution format:

```text
VocabularyID:Category:Path:Version
```

Example:

```text
core:DOMAIN.ENTITY.AGGREGATE:v1
spring:SERVICE.STATELESS:v3
```

Unknown vocabulary entries are invalid.

Resolution is deterministic.

---

# 6. Typed Semantic Hypergraph

## 6.1 Type System

SEDNA uses a typed semantic hypergraph.

Every node has:
- semantic type
- execution type
- contract type
- mutation compatibility rules

---

## 6.2 Link Types

| Link Type | Meaning |
|---|---|
| COMPOSITION | Parent-child ownership |
| DEPENDENCY | Capability requirement |
| REFERENCE | Semantic usage |
| POLICY | Constraint attachment |
| EVENT | Semantic signaling |

---

## 6.3 Compatibility Rules

Mutation compatibility requires:
- semantic compatibility
- contract compatibility
- execution profile compatibility
- constraint compatibility

Invalid graph states are rejected before execution.

---

# 7. CONTRACTS

## 7.1 Purpose

Contracts define semantic interoperability.

Contracts are capability-driven.

---

## 7.2 Grammar

```ebnf
CONTRACT :=
    PROVIDES*
    REQUIRES*
    PROTOCOL
    IO_SCHEMA
```

Example:

```text
Provides:
    USER_SERVICE@2.1

Requires:
    USER_REPOSITORY@>=1.0

Protocol:
    SYNC
```

---

## 7.3 IO Schema

Supported schema formats:
- JSON Schema
- Java Type Signature

Schema storage:
- inline for schemas <1KB
- registry reference for reusable schemas

Compatibility unit:
- semantic contract version
- schema hash

Schema selection:
- external APIs → JSON Schema
- internal JVM contracts → Java Type Signature

---

## 7.4 Capability Resolution

Resolution order:
1. exact semantic match
2. compatible version match
3. nearest semantic provider

Fallback resolution emits validation warning.

---

# 8. LINKS

## 8.1 Semantic References

SEDNA uses semantic late binding.

Links reference semantic capabilities instead of physical addresses.

Example:

```text
REQUEST:
    CAPABILITY: USER_SERVICE
    VERSION: >=2.0
```

---

## 8.2 Binding Policies

| Policy | Meaning |
|---|---|
| STRICT | Exact capability required |
| FLEXIBLE | Compatible provider allowed |
| POLYMORPHIC | Dynamic semantic resolution |

---

# 9. CONSTRAINTS

## 9.1 Purpose

Constraints define architectural rules.

Constraints propagate recursively.

Child nodes cannot weaken inherited constraints.

---

## 9.2 Examples

```text
NO_DIRECT_DB_ACCESS
MANDATORY_VALIDATION
STATELESS_ONLY
REQUIRES_TRANSACTION
```

---

## 9.3 Validation Stack

| Validation | Technology |
|---|---|
| Architecture Rules | ArchUnit |
| Dependency Validation | Spring Context Analysis |
| Compilation | Gradle |
| Integration Tests | JUnit + Testcontainers |

---

# 10. CONTEXT

## 10.1 Purpose

Contexts define semantic mutation boundaries.

Contexts reduce mutation scope and semantic entropy.

---

## 10.2 Context Types

| Context | Purpose |
|---|---|
| LOCAL | Single node scope |
| MODULE | Spring module boundary |
| DOMAIN | Business bounded context |
| ISOLATED | Sandboxed execution |

Operational semantics are defined in:
`SEDNA-CONTEXT-v1`

---

# 11. CHILDREN AND HIERARCHY

## 11.1 Recursive Composition

Nodes may recursively contain child nodes.

This enables:
- reusable motifs
- workflows
- bounded contexts
- complete systems

---

## 11.2 Folding

Repeated semantic subgraphs may be folded into motifs.

Example motifs:

```text
CRUD_SERVICE_MOTIF
AUTH_FLOW_MOTIF
VALIDATED_ENTITY_PIPELINE
```

---

## 11.3 Folding Heuristics

Folding uses:
- graph signature matching
- structural similarity
- contract compatibility
- semantic role matching

Subgraph isomorphism is approximated.

Exact NP-hard matching is intentionally avoided.

---

# 12. Execution Semantics

## 12.1 Primary Execution Model

Primary execution model:
- typed DAG execution
- supervisor boundaries
- deterministic orchestration

---

## 12.2 DAG Execution

DAG execution guarantees deterministic dependency resolution and bounded execution complexity.

Arbitrary cycles are disallowed in DAG execution.

---

## 12.3 Supervisor Nodes

Supervisor nodes isolate:
- retries
- compensation
- side effects
- transaction coordination

Regular semantic nodes remain deterministic.

---

# 13. Mutation Semantics

## 13.1 Mutation Types

| Mutation | Meaning |
|---|---|
| NODE_REPLACEMENT | Replace semantic strategy |
| CONTRACT_SUBSTITUTION | Replace compatible provider |
| CONSTRAINT_INJECTION | Add policy |
| MOTIF_FOLD | Compress structure |
| MOTIF_EXPAND | Expand folded structure |

---

## 13.2 Atomic Mutation Rules

Mutations are atomic graph transactions.

```text
BEGIN MUTATION
    apply graph delta
    validate contracts
    validate constraints
    validate graph coherence
    validate codegen probe
COMMIT | ROLLBACK
```

Failed validation triggers full rollback.

---

## 13.3 Validation Order

Validation sequence:
1. contract validation
2. constraint validation
3. graph coherence validation
4. execution validation
5. Spring code generation probe
6. compilation validation

---

# 14. Reverse Engineering

## 14.1 Purpose

Analyzer reconstructs semantic genomes from Spring Boot systems.

Reverse engineering targets semantic structure instead of source text.

---

## 14.2 Technologies

| Component | Technology |
|---|---|
| AST Analysis | Spoon |
| Bytecode Analysis | ASM |
| Dependency Graph | Spring Context Analyzer |
| Architecture Validation | ArchUnit |
| Build Analysis | Gradle Tooling API |

---

# 15. Semantic Registry

## 15.1 Purpose

The Semantic Registry stores ontology definitions.

It provides deterministic semantic decoding.

---

## 15.2 Registry Layers

| Layer | Purpose |
|---|---|
| Core Vocabulary | Spring Boot primitives |
| Domain Vocabulary | Domain semantics |
| Motif Registry | Reusable semantic graph patterns |

---

## 15.3 Versioning

Every genome stores:
- vocabulary version
- semantic registry version
- compatibility metadata

This guarantees deterministic reconstruction.

