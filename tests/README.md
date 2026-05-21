# tests module

## Tasks

| Task | Scope |
|------|--------|
| `./gradlew :tests:test` | Unit and integration tests (**excludes** `@Tag("e2e")`) |
| `./gradlew e2e` | Full E2E suite (`io.sedna.tests.e2e.*`) |

## Test tiers

| Tier | Tag / location | Runs in | Notes |
|------|----------------|---------|-------|
| **E2E** | `@Tag("e2e")`, `io.sedna.tests.e2e` | `e2e` | CLI + API; isolated `build/test-outputs/<test-id>/` |
| **Fast integration** | `io.sedna.tests` (no e2e tag) | `test` | `CiDeterminismTest`, `SpringBootReverseForwardEquivalenceTest`, `Phase14AcceptanceTest`, … |
| **Fuzz** | `EncodingFuzzTest`, `MutationFuzzTest` | `test` only | High iteration count; never duplicated in `e2e` |

### Legacy vs E2E (no duplication)

| Legacy (`test`) | E2E equivalent |
|-----------------|----------------|
| `CiDeterminismTest` | `DnaRoundtripE2eTest`, `DnaEncodeE2eTest`, `ForwardDeterminismE2eTest`, `FullDeterminismSuite` |
| `GoldenFixtureReadmeShaTest` | `DnaEncodeE2eTest` (same `GOLDEN_SHA256`) |
| `ForwardCompileIntegrationTest` | `ForwardCompileE2eTest` (disabled; use `e2e`) |
| `SpringBootReverseForwardEquivalenceTest` | `ReverseForwardEquivalenceE2eTest` (golden + `sedna-e2e-tests/cms-reference`; demos stay in `test`) |
| `Phase14AcceptanceTest` | `CoreValidationChainE2eTest` (Graphviz subset) |
| `DeterminismStressTest` | `DnaRoundtripE2eTest` (parallel stress stays in `test`) |

CLI output isolation: prefer `--clean` on `forward` / `reverse` / `train` (see `CliCleanForwardE2eTest`).

## Fixtures

Regenerate committed files under `tests/fixtures/`:

```bash
./gradlew :tests:test --tests io.sedna.tests.e2e.E2eFixtureMaterializerTest
```

See [`TODO-tests.md`](../TODO-tests.md) for the full checklist.
