package io.sedna.benchmarks;

import io.sedna.reverse.ReverseServices;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ReversePipelineBenchmark {

  private static final Path CMS_REFERENCE =
      io.sedna.core.examples.ExamplesLayout.findProjectRoot(
              Path.of("..").toAbsolutePath().normalize(), "cms-reference")
          .orElseThrow();

  @Benchmark
  public byte[] reverseCmsReference() {
    return ReverseServices.pipeline().reverse(CMS_REFERENCE).value();
  }
}
