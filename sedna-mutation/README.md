# sedna-mutation

Subtree-scoped semantic mutations with validate/commit/rollback.

## Example

```java
import io.sedna.core.Mutation;
import io.sedna.core.MutationType;
import io.sedna.mutation.MutationServices;

var engine = MutationServices.engine();
var result = engine.apply(graph, new Mutation(nodeId, MutationType.CONSTRAINT_INJECTION));
```
