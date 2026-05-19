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
62782b17910b17e153c39c7069648554e2659cb37ea26519cf303cf336122fbf
```

## Regenerate

```bash
./gradlew :sedna-dna:test --tests io.sedna.dna.GoldenFixtureTest
```

If bytes change intentionally, commit both `.sdna` and this README hash.
