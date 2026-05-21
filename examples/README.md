# SEDNA Examples

Training and test sample projects for the SEDNA pipelines. The tree is organized for many independent Gradle subprojects.

## Layout

```text
examples/
├── docs/                    Catalogs, manifests, fixture documentation
├── sedna-cms/               Reserved for future CMS samples
├── sedna-demo/              Small Spring Boot demos for profile tests
├── sedna-e2e-tests/         E2E golden DNA + cms-reference project + training manifest
└── sedna-unit-tests/        Reserved for future unit-test fixtures
```

## Categories

| Directory | Purpose |
|-----------|---------|
| `sedna-cms` | Placeholder category (E2E CMS project is under `sedna-e2e-tests`) |
| `sedna-demo` | Additional Spring Boot monoliths (`spring-demo`, `inventory-demo`, `order-demo`) |
| `sedna-e2e-tests` | `cms-reference` project, `cms-reference-fixture.sdna`, `e2e-training-projects.txt` |
| `sedna-unit-tests` | Placeholder for isolated unit-test fixtures (not used yet) |

## Documentation

All catalogs and manifests live under `examples/docs/`:

- `cms-list.csv` — external CMS catalog metadata (Java entries drive corpus scale targets)
- `training-projects.txt` — explicit project list for `sedna train --projects=`
- `training-corpus.list` — generated manifest (do not edit by hand during CI)
- `cms-reference-fixture.md` — golden SHA-256 and node documentation for E2E fixture

## Quick commands

```bash
# Reverse reference CMS
./gradlew :sedna-cli:run --args="reverse --input=examples/sedna-e2e-tests/cms-reference --output=/tmp/cms.sdna"

# Validate golden fixture
./gradlew :sedna-cli:run --args="validate --input=examples/sedna-e2e-tests/cms-reference-fixture.sdna"

# Train on all local sedna-* projects
./gradlew :sedna-cli:run --args="train --corpus=. --output=training-out"
```

Path resolution in Java: `io.sedna.core.examples.ExamplesLayout`.
