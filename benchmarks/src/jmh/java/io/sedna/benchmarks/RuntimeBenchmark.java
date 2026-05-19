package io.sedna.benchmarks;

import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
import io.sedna.runtime.RuntimeServices;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class RuntimeBenchmark {

  @Setup
  public void warmup() {
    RuntimeServices.engine().run(CmsReferenceFixtureGraph.create());
  }

  @Benchmark
  public Object runDag() {
    return RuntimeServices.engine().run(CmsReferenceFixtureGraph.create()).value();
  }
}
