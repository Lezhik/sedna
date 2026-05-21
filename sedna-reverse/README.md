# sedna-reverse

Spring Boot project → semantic DNA (JavaParser, CMS profile).

## Example

```java
import io.sedna.reverse.ReverseServices;
import java.nio.file.Path;

var dna = ReverseServices.pipeline().reverse(Path.of("examples/sedna-e2e-tests/cms-reference")).value();
```

Target: reverse analysis &lt;30s on reference project (JMH: `ReversePipelineBenchmark`).
