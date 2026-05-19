# SEDNA Execution Semantics & Runtime Model

| Field | Value |
|-------|-------|
| **Document version** | 1.0 |
| **Status** | Active |
| **Last updated** | 2026-Q2 |

---

## 1. Purpose

This document defines the runtime execution semantics of SEDNA DNA.

The goal of the runtime model is deterministic semantic execution of compact DNA structures while preserving consistency with:

- `docs/sedna_formal_semantic_specification.md` (v1.0)
- `docs/sedna_forward_pipeline_specification.md` (v1.0)
- `docs/sedna_reverse_pipeline_specification.md` (v1.0)
- `docs/sedna_training_pipeline_specification.md` (v1.0)

This specification defines:

- execution profiles
- runtime graph semantics
- deterministic scheduling
- capability resolution
- replay semantics
- checkpointing
- supervisor behavior
- consistency guarantees between Forward Pipeline and Runtime execution

---

# 2. Runtime Principles

SEDNA runtime executes semantic intent rather than generated source code.

Generated Java artifacts are execution projections of semantic DNA.

Runtime execution must remain:

- deterministic
- replayable
- mutation-safe
- contract-valid
- constraint-consistent

Non-deterministic outputs are forbidden inside canonical semantic state.

---

# 3. Runtime Inputs

## 3.1 Runtime Inputs

Runtime receives:

- compact DNA
- resolved vocabulary registry
- capability registry
- execution constraints
- execution profile definitions
- checkpoint persistence state
- runtime configuration

---

## 3.2 Runtime DNA Expansion

Compact DNA is expanded into:

- semantic nodes
- runtime contracts
- capability bindings
- execution graph
- execution scheduling plan

DNA remains canonical.

Expanded runtime graph is transient.

---

# 4. Execution Profiles

## 4.1 Execution Profile Types

| Profile | Purpose | Loops | State Persistence | Compensation |
|---|---|---|---|---|
| DAG | Pure deterministic execution | Forbidden | No | No |
| SUPERVISOR | Side-effect orchestration | Controlled | Optional | Allowed |
| STATEFUL | Long-running workflows | Controlled FSM only | Required | Allowed |

---

## 4.2 STATEFUL Runtime Semantics

STATEFUL execution supports:

- persisted execution state
- resumable execution
- deterministic replay
- workflow continuation
- checkpoint recovery

Execution token structure:

```text
ExecutionToken = SHA-256(
    NodeID +
    StateHash +
    SequenceNumber
)
```

Checkpoint persistence:

- PostgreSQL persistence
- append-only execution log
- deterministic replay reconstruction

Checkpoint failure semantics:

```text
Checkpoint failure before side effect:
abort side effect,
retry from last valid checkpoint.
```

---

## 4.3 SUPERVISOR Runtime Semantics

SUPERVISOR profile handles:

- retries
- compensation
- side effects
- external service orchestration
- controlled partial execution

Retry policy is stored inside DNA.

```text
RetryPolicy:
* max_attempts: uint8
* backoff: FIXED | EXPONENTIAL
* on_exhaustion: COMPENSATE | ABORT
```

Default policy:

```text
max_attempts = 3
backoff = EXPONENTIAL
on_exhaustion = ABORT
```

Compensation semantics:

- semantic rollback action
- deterministic compensation ordering
- supervisor-scoped recovery only

---

# 5. Runtime Graph Execution

## 5.1 Execution Graph

Runtime graph is a typed semantic hypergraph.

Runtime graph contains:

- semantic nodes
- execution edges
- capability bindings
- runtime contracts
- execution profiles

Execution graph is transient and reconstructed from DNA.

---

## 5.2 Canonical Topological Ordering

DAG execution uses deterministic topological ordering.

Canonical ordering rules:

1. dependency topology ordering
2. canonical NodeID lexicographic tiebreaker

Both Forward Pipeline and Runtime MUST use the identical ordering algorithm.

Reference:

```text
SEDNA-EXEC-v1
```

Purpose separation:

| Stage | Purpose |
|---|---|
| Forward Pipeline | validation ordering |
| Runtime | execution scheduling ordering |

Both stages must produce identical dependency topology.

---

# 6. Replay Semantics

## 6.1 Deterministic Replay

Replay reconstructs semantic execution state from:

- DNA
- checkpoint log
- execution tokens
- deterministic scheduling

Replay guarantees:

- identical execution ordering
- identical semantic transitions
- identical contract resolution

---

## 6.2 Replay Exclusions

Replay excludes:

- timestamps
- random generators
- external HTTP payloads
- external mutable systems

Excluded values are treated as non-canonical runtime state.

---

## 6.3 Replay Categories

| Component | Replayable |
|---|---|
| Semantic graph | Yes |
| Contract resolution | Yes |
| DAG scheduling | Yes |
| FSM state | Yes |
| External HTTP response | No |
| Timestamp values | No |
| Random UUID | No |

---

# 7. Capability Resolution

## 7.1 Capability Binding

Runtime resolves semantic references into executable bindings.

Resolution stages:

1. strict match
2. version-compatible match
3. fallback semantic adaptation

---

## 7.2 Resolution Policies

| Policy | Behavior |
|---|---|
| STRICT | exact capability required |
| FLEXIBLE | compatible version allowed |
| POLYMORPHIC | multiple providers allowed |

---

## 7.3 Resolution Constraints

All providers must satisfy:

- identical contract version
- constraint compatibility
- execution profile compatibility

Constraint violations trigger:

- rollback
- capability re-resolution
- runtime abort if unresolved

---

## 7.4 POLYMORPHIC Determinism

POLYMORPHIC capability resolution remains deterministic.

Resolution order:

1. capability priority weight
2. canonical provider NodeID lexicographic tiebreaker

Parallel providers are allowed only if:

- identical contract version
- deterministic merge semantics
- execution ordering remains canonical

---

# 8. Checkpointing

## 8.1 Checkpoint Purpose

Checkpointing enables:

- workflow continuation
- deterministic replay
- failure recovery
- resumable STATEFUL execution

---

## 8.2 Checkpoint Rules

Checkpoint boundaries:

- before external side effects
- after FSM state transitions
- before compensation execution
- after successful semantic commit

Checkpoint persistence:

- append-only
- immutable replay history
- deterministic ordering

---

# 9. Determinism Model

## 9.1 Deterministic Runtime Components

Deterministic components:

- graph expansion
- capability resolution
- DAG scheduling
- contract validation
- FSM transitions
- checkpoint ordering

---

## 9.2 Semi-Deterministic Components

Semi-deterministic components:

- external HTTP systems
- asynchronous execution timing
- LLM enrichment
- database latency

These components are excluded from canonical semantic state.

---

# 10. Runtime Consistency with Forward Pipeline

## 10.1 Shared Runtime Semantics

Forward Pipeline and Runtime share:

- semantic graph structure
- contract system
- capability resolution rules
- canonical ordering
- execution profiles

---

## 10.2 DAG Consistency

Forward Pipeline:

- validates dependency graph
- validates cycle safety
- validates execution profile compatibility

Runtime:

- executes validated graph
- preserves canonical ordering
- schedules executable nodes

Both stages must produce identical dependency topology.

---

## 10.3 STATEFUL Consistency

STATEFUL runtime semantics must remain identical between:

- generated code
- semantic runtime graph
- replay reconstruction

FSM transitions are canonical semantic state.

Generated Java state machine code is projection only.

---

# 11. Validation & Execution Rules

## 11.1 Runtime Validation Order

Runtime validation order:

1. contract validation
2. constraint validation
3. capability resolution
4. execution profile validation
5. scheduling validation
6. execution start

---

## 11.2 Validation Failure Handling

Validation failure triggers:

- runtime abort
- rollback
- checkpoint restoration
- supervisor compensation if allowed

---

## 11.3 Partial Execution Rules

DAG profile:

```text
No partial semantic execution allowed.
```

SUPERVISOR profile:

```text
Controlled partial execution allowed
through deterministic compensation.
```

STATEFUL profile:

```text
Checkpoint-resumable partial execution allowed.
```

---

# 12. Failure Semantics

## 12.1 DAG Failure

DAG failure behavior:

- immediate abort
- rollback
- no compensation
- deterministic failure boundary

---

## 12.2 SUPERVISOR Failure

SUPERVISOR failure behavior:

- retry execution
- compensation execution
- rollback after exhaustion
- deterministic retry ordering

---

## 12.3 STATEFUL Failure

STATEFUL failure behavior:

- checkpoint recovery
- replay continuation
- workflow resumption
- persisted state reconstruction

---

# 13. Runtime Storage Model

## 13.1 Canonical Storage

Stored canonically:

- compact DNA
- checkpoint logs
- execution tokens
- semantic transitions
- FSM states

Not stored canonically:

- generated Java AST
- runtime JVM objects
- temporary execution buffers

---

## 13.2 Persistence Strategy

Persistence technologies:

| Purpose | Technology |
|---|---|
| Checkpoints | PostgreSQL |
| DNA storage | Binary TLV |
| Replay log | Append-only journal |
| FSM persistence | PostgreSQL |

---

# 14. Runtime Technologies

## 14.1 Runtime Stack

| Runtime Concern | Technology |
|---|---|
| Dependency Injection | Spring Boot |
| DAG execution | Custom DAG executor |
| FSM execution | Spring State Machine |
| Async execution | Project Reactor |
| Persistence | PostgreSQL |
| Serialization | CBOR / TLV |

---

## 14.2 FSM Runtime Constraints

Spring State Machine is used only as FSM execution substrate.

Canonical FSM semantics remain defined by SEDNA DNA.

Spring persistence mechanisms are wrapped by SEDNA persistence layer.

SEDNA runtime remains canonical.

---

## 14.3 Async Runtime Determinism

Project Reactor schedulers must preserve canonical execution ordering.

Parallel branches use dependency-aware Reactor sequencing.

Non-deterministic scheduler ordering is forbidden inside canonical semantic execution.

---

# 15. Runtime Principle

SEDNA runtime executes semantic intent through deterministic semantic scheduling.

Generated code is execution projection only.

