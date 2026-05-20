# sedna-cli

Command-line interface for all pipelines.

## Example

```bash
./gradlew :sedna-cli:run --args="help"
./gradlew :sedna-cli:run --args="validate --input=examples/sedna-e2e-tests/cms-reference-fixture.sdna"
./gradlew :sedna-cli:run --args="diff --left=a.sdna --right=b.sdna --format=json"
./gradlew :sedna-cli:run --args="visualize --input=examples/sedna-e2e-tests/cms-reference-fixture.sdna --output=/tmp/cms.dot"
./gradlew :sedna-cli:run --args="run --input=examples/sedna-e2e-tests/cms-reference-fixture.sdna --checkpoint-dir=/tmp/cp"
./gradlew :sedna-cli:run --args="replay --checkpoint-dir=/tmp/cp --format=json"
```

Installable distribution: `./gradlew :sedna-cli:installDist`.

Operator guide: [docs/operator-guide.md](../docs/operator-guide.md)
