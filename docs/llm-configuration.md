# LLM Configuration (OpenRouter)

SEDNA uses an **out-of-process** HTTP client for optional method-body synthesis and label enrichment. In-process LLM SDKs are forbidden.

| Variable | Default | Description |
|----------|---------|-------------|
| `SEDNA_LLM_ENABLED` | `false` | Set `true` to enable HTTP calls (CI must keep `false`) |
| `SEDNA_LLM_BASE_URL` | `https://openrouter.ai/api/v1` | OpenRouter OpenAI-compatible API base |
| `SEDNA_LLM_MODEL` | `openai/gpt-4o-mini` | Chat model id |
| `SEDNA_LLM_TIMEOUT_MS` | `30000` | HTTP read timeout (milliseconds) |
| `OPENROUTER_API_KEY` | — | Required when LLM is enabled |

## Retry policy

- No automatic retries in the client (determinism and bounded CI time).
- On HTTP error, timeout, oversize payload (>64KB request JSON), or sanitized unsafe response → **empty method body skeleton**; pipeline continues.
- Structure (graph, contracts, NodeIDs) is never modified by LLM output.

## Security

- Responses pass through `LlmResponseSanitizer` (blocks `ProcessBuilder`, `exec`, shell patterns).
- Request/response size caps apply before parsing.

## Example (local)

```bash
export SEDNA_LLM_ENABLED=true
export OPENROUTER_API_KEY=sk-or-...
./gradlew :sedna-cli:run --args="forward --input=examples/cms-reference-fixture.sdna --output=generated"
```
