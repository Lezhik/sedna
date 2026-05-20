# Examples documentation

Metadata and manifests for the `examples/` tree. Trainable Gradle projects live under `examples/sedna-* /`; this folder holds catalogs and golden-fixture documentation only.

| File | Description |
|------|-------------|
| `cms-list.csv` | External CMS catalog (language column filters Java training targets) |
| `training-projects.txt` | One repo-relative path per line for `sedna train --projects=` |
| `training-corpus.list` | Auto-generated list of local `sedna-*` projects (deterministic order) |
| `cms-reference-fixture.md` | Golden SHA-256 and semantics for `../sedna-e2e-tests/cms-reference-fixture.sdna` |
