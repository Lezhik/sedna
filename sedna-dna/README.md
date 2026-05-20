# sedna-dna

SEDNA-BIN-v1 TLV encoder/decoder and NodeID hashing.

## Example

```java
import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;

byte[] dna = DnaServices.encoder().encode(CmsReferenceFixtureGraph.create()).value();
var graph = DnaServices.decoder().decode(dna).value();
```

Golden fixture: `examples/sedna-e2e-tests/cms-reference-fixture.sdna` (see `examples/docs/cms-reference-fixture.md`).
