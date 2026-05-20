# sedna-training

Deterministic training dataset pipeline (Phase 6 / Phase 13).

## Usage

```java
var loader = new ProjectListLoader();
var projects = loader.load(Path.of("projects.txt")).value();
var dataset = TrainingServices.pipeline().train(projects).value();

var index = SemanticEmbeddingIndex.fromDataset(dataset);
var neighbors = index.nearestNeighbors(
    dataset.projects().getFirst().projectPath(),
    dataset.projects().getFirst().embeddings().getFirst().nodeId(),
    dataset.projects().getFirst().embeddings().getFirst().embeddingHex(),
    3);

var artifacts = new TrainingDatasetWriter().write(dataset, Path.of("out/training")).value();
// dataset.manifest, dataset.manifest.sha256, reproducibility.report
```

Corpus mode (all `examples/*` with `src/main/java` + `cms-list.csv` catalog metadata):

```java
TrainingServices.pipeline().trainCorpus(Path.of(".").toAbsolutePath().normalize());
```

CLI:

```text
sedna train --projects=examples/training-projects.txt [--output=out/training]
sedna train --corpus=. [--output=out/training]
```

## Artifacts

| File | Purpose |
|------|---------|
| `dataset.manifest` | Canonical dataset summary + per-project fingerprints |
| `dataset.manifest.sha256` | Manifest checksum |
| `reproducibility.report` | Trajectory/index checksums + `DETERMINISTIC_REPLAY_READY` gate |

## Embedding retrieval (Phase 13 P1)

`SemanticEmbeddingIndex` implements **pure Java** brute-force cosine similarity over SEDNA-EMBED-v1 hex vectors (32-byte SHA-256). Optional FAISS can wrap the same vectors externally; the canonical path stays deterministic in-JVM.

## Rules

- One project folder per line in the projects list; never merge graphs across projects.
- Git commit order is oldest-first (JGit log reversed).
- Per-commit snapshots when history has ≥2 commits; mutations labeled `commitHash:label`.
- Embeddings are SHA-256 over vocabulary path + contract signature (no LLM).
- Registry proposals use deterministic conflict resolution (`SKIP_EXACT`, `APPEND_VERSION`, `MANUAL_REVIEW`).
- Corpus-level registry proposal conflicts fail `train()` via `RegistryProposalCorpusValidator`.

## Minimum dataset

Target corpus: **20–30** Spring Boot CMS monoliths. Acceptance gates: `Phase13AcceptanceTest` (synthetic 20+ projects, 10+ commits, 500+ mutation rows).
