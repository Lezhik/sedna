# Invalid DNA fixtures

| File | Error (validate CLI) |
|------|----------------------|
| `invalid-graph.sdna` | `VALIDATION_FAILED` — orphan link target |
| `duplicate-node.sdna` | `VALIDATION_FAILED` — duplicate node id |
| `unknown-vocab.sdna` | `UNKNOWN_VOCAB` — unknown vocabulary id `nope` |
| `invalid-dna-magic.sdna` | `INVALID_DNA` — corrupt magic bytes |
| `cyclic-dependency.sdna` | optional; may pass graph validation (not in E2E matrix) |

Regenerate all fixtures:

```bash
./gradlew :tests:test --tests io.sedna.tests.e2e.E2eFixtureMaterializerTest
```
