package io.sedna.dna;

/** Factory for default DNA codec implementations. */
public final class DnaServices {

  private DnaServices() {}

  public static DnaEncoder encoder() {
    return new DefaultDnaEncoder();
  }

  public static DnaDecoder decoder() {
    return new DefaultDnaDecoder();
  }
}
