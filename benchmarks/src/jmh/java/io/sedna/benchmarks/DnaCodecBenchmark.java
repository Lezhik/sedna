package io.sedna.benchmarks;

import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaDecoder;
import io.sedna.dna.DnaEncoder;
import io.sedna.dna.DnaServices;
import io.sedna.dna.fixture.CmsReferenceFixtureGraph;
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
public class DnaCodecBenchmark {

  private DnaEncoder encoder;
  private DnaDecoder decoder;
  private SemanticGraph graph;
  private byte[] dna;

  @Setup
  public void setup() {
    encoder = DnaServices.encoder();
    decoder = DnaServices.decoder();
    graph = CmsReferenceFixtureGraph.create();
    dna = encoder.encode(graph).value();
  }

  @Benchmark
  public byte[] encode() {
    return encoder.encode(graph).value();
  }

  @Benchmark
  public SemanticGraph decode() {
    return decoder.decode(dna).value();
  }
}
