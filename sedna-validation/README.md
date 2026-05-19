# sedna-validation

Graph, contract, and mutation safety validation.

## Example

```java
import io.sedna.registry.InMemorySemanticRegistry;
import io.sedna.validation.CompositeValidationEngine;

var engine = CompositeValidationEngine.standard(InMemorySemanticRegistry.bootstrap());
var report = engine.validate(graph).value();
```
