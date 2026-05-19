# sedna-training

Deterministic training dataset pipeline (Phase 6 / v0.6).

## Usage

```java
var loader = new ProjectListLoader();
var projects = loader.load(Path.of("projects.txt")).value();
var dataset = TrainingServices.pipeline().train(projects).value();
new TrainingDatasetWriter().write(dataset, Path.of("out/training"));
```

CLI:

```text
sedna train --projects=projects.txt [--output=out/training]
```

## Rules

- One project folder per line in the projects list; never merge graphs across projects.
- Git commit order is oldest-first (JGit log reversed).
- Embeddings are SHA-256 over vocabulary path + contract signature (no LLM).
- Registry proposals use deterministic conflict resolution (`SKIP_EXACT_MATCH`, `APPEND_NEW_VERSION`, `MANUAL_REVIEW`).

## Minimum dataset

Target corpus: **20–30** Spring Boot CMS monoliths (`projects.txt`). MVP reverse profile: `examples/cms-reference` layout.
