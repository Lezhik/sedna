# sedna-registry

Versioned vocabulary resolution.

## Example

```java
import io.sedna.core.VocabRef;
import io.sedna.registry.InMemorySemanticRegistry;

var registry = InMemorySemanticRegistry.bootstrap();
var definition = registry.resolve(new VocabRef("core", "DOMAIN.ENTITY.AGGREGATE", "v1")).value();
```
