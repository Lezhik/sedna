# SEDNA Examples

Training and test sample projects for the SEDNA pipelines. The tree is organized for many independent Gradle subprojects.

## Layout

```text
examples/
├── docs/                    Catalogs, manifests, fixture documentation
├── sedna-cms/               CMS reference applications
├── sedna-demo/              Small Spring Boot demos for profile tests
├── sedna-e2e-tests/         Golden DNA fixtures for end-to-end CI
└── sedna-unit-tests/        Reserved for future unit-test fixtures
```

## Categories

| Directory | Purpose |
|-----------|---------|
| `sedna-cms` | Reference CMS (e.g. `cms-reference`) for reverse/forward equivalence |
| `sedna-demo` | Additional Spring Boot monoliths (`spring-demo`, `inventory-demo`, `order-demo`) |
| `sedna-e2e-tests` | Hand-authored `.sdna` golden bytes and related E2E assets |
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
./gradlew :sedna-cli:run --args="reverse --input=examples/sedna-cms/cms-reference --output=/tmp/cms.sdna"

# Validate golden fixture
./gradlew :sedna-cli:run --args="validate --input=examples/sedna-e2e-tests/cms-reference-fixture.sdna"

# Train on all local sedna-* projects
./gradlew :sedna-cli:run --args="train --corpus=. --output=training-out"
```

Path resolution in Java: `io.sedna.core.examples.ExamplesLayout`.
