# sedna-runtime

Deterministic DAG execution and replay.

## Example

```java
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.runtime.RuntimeServices;

var trace = RuntimeServices.engine().run(CmsReferenceFixtureGraph.create()).value();
```
