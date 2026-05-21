# sedna-e2e-tests

End-to-end assets and reference projects for CI (`./gradlew e2e`).

| Asset / project | Purpose |
|-----------------|---------|
| `cms-reference-fixture.sdna` | Hand-authored minimum CMS graph (DNA bytes, determinism gates) |
| `cms-reference/` | Spring Boot CMS (`io.sedna.cms`) for reverse, forward compile, and chain tests |
| `e2e-training-projects.txt` | Project list for `TrainingDatasetE2eTest` (`sedna train --projects=`) |

Documentation: [../docs/cms-reference-fixture.md](../docs/cms-reference-fixture.md).
