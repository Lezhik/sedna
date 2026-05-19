# SEDNA Forward Pipeline Specification

| Field | Value |
|-------|-------|
| **Document version** | 1.0 |
| **Status** | Active |
| **Last updated** | 2026-Q2 |

---

## 1. Purpose
This document defines the **Forward Execution Pipeline** for SEDNA DNA. It specifies how compact semantic genome representations are transformed into executable Java Spring Boot systems via deterministic and semi-deterministic stages.

The pipeline operates on a strict principle:
> DNA → Semantic Graph → Resolved Execution Plan → Generated Code

---

## 2. Input / Output Definition

### Input
- Compact SEDNA DNA (v1 binary TLV format)
- Vocabulary Registry (versioned)
- Contract Registry (versioned)

### Output
- Executable Spring Boot project (Gradle-based)
- Fully generated Java code + configuration
- Optional test scaffold artifacts

---

## 3. Step 1 — DNA Parsing

### Objective
Decode binary DNA into structured semantic nodes.

### Input
- Binary DNA (TLV, little-endian)

### Output
- Raw Semantic Nodes (unresolved references)

### Algorithm / Tech
- TLV Decoder
- SHA-256 NodeID validation
- Vocabulary lookup (dictionary-based resolution)

---

## 4. Step 2 — Registry Resolution

### Objective
Resolve semantic references into typed semantic nodes.

### Input
- Raw Semantic Nodes
- Vocabulary Registry

### Output
- Resolved Semantic Nodes (capability-referenced)

### Algorithm / Tech
- Dictionary-based semantic resolution
- Versioned ontology lookup
- Fallback semantic matching (approximate match)

---

## 5. Step 3 — Hypergraph Construction

### Objective
Construct semantic hypergraph from resolved nodes.

### Input
- Resolved Semantic Nodes

### Output
- Typed Semantic Hypergraph (unexecuted)

### Key Rule
- LINKS remain semantic references at this stage (NOT physical edges)
- Physical edges are materialized in Step 4

### Algorithm / Tech
- Graph assembly engine
- Capability reference resolver (deferred binding)

---

## 6. Step 4 — Contract Resolution

### Objective
Resolve capabilities into concrete service bindings.

### Input
- Semantic Hypergraph
- Contract Registry

### Output
- Bound Execution Graph (partially materialized)

### Algorithm / Tech
- Capability-based service discovery
- Contract version matching (>=, compatible range)
- Semantic late binding resolution

---

## 7. Step 5 — Constraint Propagation

### Objective
Validate semantic constraints across graph structure.

### Input
- Bound Execution Graph

### Output
- Constraint-validated Graph

### Algorithm / Tech
- Constraint propagation engine
- Policy inheritance evaluation
- Conflict detection (hard fail on violation)

### Rule
- Contract validation precedes constraint validation
- Constraint invalidation triggers rollback via SEDNA v0.2 mutation transaction model (Section 13.2)

---

## 8. Step 6 — Execution Planning

### Objective
Produce execution ordering of semantic graph.

### Input
- Constraint-validated Graph

### Output
- Execution Plan (ordered DAG + supervisors)

### Algorithm / Tech
- Topological sort (execution-specific)
- Execution profile evaluation:
  - DAG
  - SUPERVISOR
  - STATEFUL

---

## 9. Step 7 — Code Generation

### Objective
Generate executable Spring Boot code.

### Input
- Execution Plan

### Output
- Java source code + configuration files

### Code Generation Strategy

| Tool | Usage |
|------|------|
| JavaPoet | Structural code (classes, interfaces, annotations) |
| Mustache | Boilerplate, configuration, Gradle, YAML |

---

## 10. LLM Integration Layer

### Objective
Inject controlled non-deterministic generation for method bodies only.

### Input
- Semantic Node Definition
- Structural Code Skeleton (from JavaPoet)

### Output
- Method implementations (pure function bodies)

### Rules
- LLM is NOT allowed to modify structure
- LLM cannot introduce new dependencies
- Output must pass compilation and unit tests

### Failure Handling
- LLM failure → fallback to empty method body skeleton

---

## 11. Determinism Rules

### Deterministic Components
- DNA parsing
- registry resolution
- graph construction
- contract resolution
- constraint propagation
- execution ordering

### Non-deterministic Component
- LLM method body generation (bounded scope only)

---

## 12. LINKS Materialization Rule

- LINKS are semantic capability references during graph construction
- They are materialized into physical edges ONLY during Contract Resolution (Step 4)
- No direct pointer resolution occurs in earlier stages

---

## 13. Validation Ordering

1. Contract validation
2. Constraint validation
3. Graph coherence validation
4. Execution feasibility check

Failure at any stage triggers rollback

---

## 14. Execution Principle

The Forward Pipeline is deterministic until the LLM boundary.
Everything structural is reproducible from DNA.
Only method bodies are probabilistic and replaceable.

---

## 15. Final Statement

This specification defines a reproducible transformation system from semantic genome to executable software.
It prioritizes structural determinism, capability-based linking, and controlled non-deterministic synthesis.

