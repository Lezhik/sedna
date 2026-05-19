# SEDNA Technical Assignment v1
## AI-Agent-Oriented Development Specification

---

# 1. Purpose

This document defines implementation requirements for the SEDNA platform.

The target audience is autonomous AI development agents with minimal human supervision.
The specification therefore prioritizes:

- deterministic implementation rules
- strict module boundaries
- machine-verifiable acceptance criteria
- canonical DTO definitions
- explicit bootstrap order
- additive-only interface evolution

This document intentionally avoids duplicating semantic rules already defined in:

- SEDNA Formal Semantic Specification
- SEDNA Forward Pipeline Specification
- SEDNA Reverse Pipeline Specification
- SEDNA Training Pipeline Specification
- SEDNA Execution Semantics & Runtime Model

---

# 2. Scope

## 2.1 Included

The MVP implementation includes:

- DNA binary storage engine
- semantic graph model
- forward pipeline
- reverse pipeline
- training pipeline
- runtime execution engine
- registry subsystem
- mutation engine
- deterministic validation engine
- persistence layer

## 2.2 Excluded From MVP

The following are explicitly out of scope:

- distributed runtime execution
- Kafka integration
- multi-node coordination
- cloud orchestration
- IntelliJ plugin
- visualization dashboards
- multi-language code generation
- Kubernetes deployment orchestration

Operational MVP constraints are defined in:

- SEDNA Formal Semantic Specification v1, Section 15

---

# 3. Technology Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Build | Gradle |
| Binary serialization | Custom TLV |
| Graph processing | JGraphT |
| Java parsing | JavaParser |
| Code generation | JavaPoet |
| Templates | Mustache |
| Persistence | PostgreSQL |
| Async execution | Project Reactor |
| FSM runtime | Spring State Machine |
| Testing | JUnit 5 |
| Benchmarking | JMH |
| LLM communication | HTTP API |

---

# 4. Repository Structure

```text
sedna/
 ├── sedna-core/
 ├── sedna-storage/
 ├── sedna-registry/
 ├── sedna-forward/
 ├── sedna-reverse/
 ├── sedna-training/
 ├── sedna-runtime/
 ├── sedna-mutation/
 ├── sedna-validation/
 ├── sedna-persistence/
 ├── examples/
 └── benchmarks/
```

---

# 5. Bootstrap Order

Bootstrap ordering is mandatory.

Circular dependencies between registry loading and DNA decoding are forbidden.

Required startup order:

```text
1. Load embedded core vocabulary
2. Initialize canonical comparators
3. Initialize DNA decoder
4. Load registry extensions
5. Initialize pipelines
6. Initialize runtime engine
```

---

# 6. Core Interfaces And DTOs

## 6.1 Canonical DTO Definitions

All modules MUST import DTOs from `sedna-core`.

Redefinition is forbidden.

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

## 6.2 Core Interfaces

```java
interface DnaDecoder {
    SemanticGraph decode(byte[] dna);
}

interface DnaEncoder {
    byte[] encode(SemanticGraph graph);
}

interface SemanticRegistry {
    SemanticDefinition resolve(VocabRef ref);
}

interface MutationEngine {
    MutationResult apply(SemanticGraph graph, Mutation mutation);
}

interface RuntimeScheduler {
    ExecutionPlan build(SemanticGraph graph);
}
```

## 6.3 Error Boundary Contract

Raw exceptions crossing module boundaries are forbidden.

All module APIs MUST use:

```java
Result<T, SemanticError>
```

Canonical error structure:

```java
record SemanticError(
    ErrorCode code,
    long nodeId,
    String message
) {}
```

---

# 7. Determinism Rules

## 7.1 Canonical Ordering

Unordered iteration is forbidden.

Forbidden:

- HashMap iteration
- HashSet iteration
- non-deterministic stream ordering

Allowed:

- LinkedHashMap
- TreeMap
- ImmutableList
- ImmutableMap

Canonical ordering utility:

```text
CanonicalOrdering.comparator()
```

Ordering rule:

```text
1. dependency topology
2. canonical node ID lexicographic ordering
```

## 7.2 Stable Identity

Node IDs MUST remain stable across:

- JVM restarts
- repeated encoding
- repeated decoding
- replay execution

---

# 8. Module Versioning

Each module MUST publish semantic version information in:

```text
gradle.properties
```

Versioning rules:

```text
breaking changes -> major version increment
additive interface changes -> minor version increment
internal implementation changes -> patch increment
```

---

# 9. Mutation Requirements

## 9.1 Supported Operations

Required mutation operations:

- node insertion
- node deletion
- subtree replacement
- motif folding
- contract upgrade
- constraint propagation

Subtree replacement definition:

```text
subtree = hierarchical CHILDREN subgraph rooted at a node
replacement scope = entire subtree including semantic links
```

## 9.2 Mutation Safety

All mutations MUST support:

- rollback
- atomic validation
- deterministic ordering
- contract re-resolution

---

# 10. Runtime Constraints

## 10.1 Phase Restrictions

Phase restrictions are mandatory.

```text
Phase 1-3:
- DAG profile only

Phase 4:
- DAG
- SUPERVISOR
- STATEFUL
```

## 10.2 Runtime Requirements

Runtime engine MUST support:

- deterministic scheduling
- checkpoint recovery
- replay execution
- compensation execution
- execution token persistence

---

# 11. LLM Integration Rules

## 11.1 Allowed Usage

LLM usage is allowed only for:

- semantic enrichment
- UNKNOWN node classification
- method body generation
- semantic suggestion generation

## 11.2 Forbidden Usage

LLM usage is forbidden for:

- NodeID generation
- graph topology construction
- contract validation
- binary serialization
- canonical ordering
- persistence decisions

## 11.3 Sandbox Model

MVP sandbox policy:

```text
LLM execution MUST run as separate process via HTTP API.
In-process LLM execution is forbidden.
```

---

# 12. Dataset Requirements

Training datasets MUST:

- use Git history
- preserve commit ordering
- preserve semantic transitions
- avoid cross-project semantic reconstruction

Git analysis MUST operate per project folder.

Whole-repository semantic reconstruction is forbidden.

---

# 13. Development Phases

## 13.1 Phase 1 — Core Infrastructure

### Requires

None.

### Deliverables

- canonical DTOs
- DNA encoder
- DNA decoder
- registry bootstrap
- validation engine

### Acceptance Criteria

```text
encode(decode(dna)) == dna

Repeated encode(graph) produces identical bytes

NodeID stability preserved across JVM restarts

registry.resolve(core reference) != null
```

---

## 13.2 Phase 2 — Forward Pipeline

### Requires

Phase 1 complete.

### Deliverables

- semantic graph expansion
- contract resolution
- code generation
- deterministic planning

### Acceptance Criteria

```text
same DNA produces identical SemanticGraph

same SemanticGraph produces identical generated code

contract validation succeeds deterministically
```

---

## 13.3 Phase 3 — Reverse Pipeline

### Requires

Phase 1 complete.

Minimum Phase 2 requirement:

- SemanticGraph model stable
- contract resolution working
- code generation not required

Phase 3 and Phase 4 may execute in parallel.

### Deliverables

- Java parsing
- semantic extraction
- motif folding
- DNA reconstruction

### Acceptance Criteria

```text
reverse(forward(dna)) preserves semantic equivalence
```

Semantic equivalence definition:

```text
- identical node count
- identical contract set
- identical constraint set
- NodeID set equality
- motif references resolve to equivalent subgraphs
```

---

## 13.4 Phase 4 — Runtime Engine

### Requires

Phase 1 complete.

Phase 2 partial:

- execution planning
- graph scheduling

### Deliverables

- DAG runtime
- SUPERVISOR runtime
- STATEFUL runtime
- checkpoint persistence

### Acceptance Criteria

```text
replay execution preserves deterministic topology

checkpoint restore resumes identical execution state

compensation ordering deterministic
```

---

## 13.5 Phase 5 — Training Pipeline

### Requires

Phase 3 complete.

### Deliverables

- semantic trajectory extraction
- registry update engine
- embedding generation
- dataset analysis

### Acceptance Criteria

```text
identical Git history produces identical trajectories

registry conflicts resolved deterministically

embedding generation deterministic
```

---

# 14. Performance Targets

Baseline environment:

```text
CPU: 4-core
RAM: 16GB
Storage: SSD
JVM: warmed
```

Reference project:

```text
examples/cms-reference
```

Performance targets:

| Operation | Target |
|---|---|
| DNA decode | <100ms |
| Forward pipeline | <5s |
| Reverse analysis | <30s |
| Runtime scheduling | <50ms |
| Registry lookup | <1ms |

---

# 15. Testing Requirements

Required test categories:

- round-trip tests
- determinism tests
- replay tests
- rollback tests
- mutation tests
- contract validation tests
- benchmark tests

All public interfaces MUST have:

- unit tests
- integration tests
- deterministic replay tests

---

# 16. Security Requirements

Required restrictions:

- no dynamic bytecode execution
- no arbitrary shell execution
- no runtime classloader mutation
- no unrestricted filesystem access

LLM outputs MUST be validated before execution.

---

# 17. AI-Agent Coordination Rules

## 17.1 Contract-First Development

Agents MUST implement:

```text
DTOs -> interfaces -> validation -> implementation
```

Implementation before contract definition is forbidden.

## 17.2 Interface Evolution Policy

After Phase 2:

```text
additive interface changes allowed
breaking changes forbidden
all interface changes require validation rerun
```

## 17.3 Public API Rules

Public APIs MUST:

- avoid raw collections
- avoid mutable DTOs
- use canonical comparators
- preserve deterministic ordering

Use:

```text
ImmutableList / ImmutableMap
or unmodifiable wrappers
```

## 17.4 Version Coordination

Every module MUST expose semantic version metadata.

Agents MUST verify dependency compatibility before implementation.

---

# 18. Post-MVP Components

Explicitly postponed:

- IntelliJ plugin
- visualization UI
- distributed runtime
- cloud orchestration
- advanced optimization engine
- autonomous mutation planning

---

# 19. Documentation Requirements

Every module MUST contain:

- README
- API documentation
- deterministic behavior notes
- benchmark instructions
- replay validation instructions

---

# 20. Final Requirement

Non-deterministic behavior inside canonical DNA generation, graph construction, contract resolution, runtime ordering, and registry updates is forbidden.

