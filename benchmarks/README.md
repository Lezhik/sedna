# SEDNA JMH Benchmarks

Performance gates referenced by E2E plan (E2E-026–028):

| Benchmark | Target (warmed JVM) |
|-----------|---------------------|
| `DnaCodecBenchmark` | DNA decode p99 &lt; 100 ms |
| `ForwardPipelineBenchmark` | Forward p99 &lt; 5 s |
| `ReversePipelineBenchmark` | Reverse p99 &lt; 30 s |

Run:

```bash
./gradlew jmh
```

These are **not** part of `./gradlew e2e` (JUnit); run on main/nightly CI separately.
