# CMS Reference DNA Fixture

Canonical minimum graph for forward/reverse equivalence tests (Phase 2–3).

| Field | Value |
|-------|-------|
| File | `cms-reference-fixture.sdna` |
| Format | SEDNA-BIN-v1 TLV |
| Nodes | 1× ENTITY, 1× SERVICE, 1× CONTROLLER |
| Links | 1× DEPENDENCY (CONTROLLER → SERVICE) |
| Contracts | SERVICE provides `USER_SERVICE@1.0`, requires `USER_REPOSITORY@>=1.0` |
| Registry | `core:1.0` embedded vocabulary |

## SHA-256 (golden bytes)

Updated automatically when `GoldenFixtureTest` runs after fixture regeneration.

```
6d75d8431baaac07398e39448b19db34b62cc6df7eb84cbdc1484d5c2d7ed8f5
```

## NodeIDs (canonical SHA-256 / 64-bit)

Derived via `NodeIdHasher` from semantic content (regenerate with `CmsReferenceFixtureGraph` if vocabulary changes).

## Regenerate

```bash
./gradlew :sedna-dna:test --tests io.sedna.dna.GoldenFixtureTest
```

If bytes change intentionally, commit both `.sdna` and this README hash.
