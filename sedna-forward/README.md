# sedna-forward

DNA → Spring Boot project (JavaPoet + Mustache).

## Example

```java
import io.sedna.forward.ForwardServices;
import io.sedna.forward.llm.DisabledLlmClient;
import io.sedna.registry.InMemorySemanticRegistry;

var pipeline = ForwardServices.pipeline(InMemorySemanticRegistry.bootstrap(), DisabledLlmClient.INSTANCE);
var result = pipeline.run(dnaBytes);
```

Target: forward reconstruction &lt;5s on `examples/sedna-e2e-tests/cms-reference` (JMH: `ForwardPipelineBenchmark`).
