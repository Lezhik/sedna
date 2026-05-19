# sedna-persistence

Checkpoint storage (in-memory and JDBC/PostgreSQL).

## Example

```java
import io.sedna.persistence.InMemoryCheckpointStore;

var store = new InMemoryCheckpointStore();
store.append(graphSnapshotRef, executionToken);
```
