# Contributing to SEDNA

## Bootstrap order (mandatory)

```text
1. sedna-core
2. sedna-dna
3. sedna-registry
4. sedna-validation
5. sedna-forward ∥ sedna-reverse (after validation)
6. sedna-runtime
7. sedna-mutation
8. sedna-training
9. sedna-cli
```

Do not skip this order when adding cross-module APIs.

## Module graph

```text
sedna-core
  ├── sedna-dna
  ├── sedna-registry
  └── sedna-validation
        ├── sedna-forward
        ├── sedna-reverse
        ├── sedna-mutation
        ├── sedna-runtime
        └── sedna-training → sedna-cli
```

`tests` and `benchmarks` aggregate integration, ArchUnit, fuzz, and JMH.

## Build

```bash
./gradlew build
./gradlew :tests:test
./gradlew :benchmarks:jmh
```

## Rules

- Canonical DTOs only in `sedna-core`.
- Public boundaries return `Result<T, SemanticError>`.
- Deterministic ordering via `CanonicalOrdering`.
- No `HashMap`/`HashSet` in `sedna-dna`, `sedna-forward`, `sedna-runtime` production code.
- Do not change golden JSON/CLI args without agreement.

## PostgreSQL (runtime dev)

```bash
docker compose up -d
```

Uses `docker-compose.yml` (port 5432, database `sedna`).
