package io.sedna.benchmarks;

import io.sedna.core.VocabRef;
import io.sedna.registry.InMemorySemanticRegistry;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class RegistryLookupBenchmark {

  private InMemorySemanticRegistry registry;
  private VocabRef ref;

  @Setup
  public void setup() {
    registry = InMemorySemanticRegistry.bootstrap();
    ref = new VocabRef("core", "DOMAIN.ENTITY.AGGREGATE", "v1");
  }

  @Benchmark
  public Object lookup() {
    return registry.resolve(ref).value();
  }
}
