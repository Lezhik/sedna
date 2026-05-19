package io.sedna.validation;

import io.sedna.core.ErrorCode;
import io.sedna.core.Result;
import io.sedna.core.SemanticError;
import io.sedna.core.SemanticGraph;
import io.sedna.dna.DnaDecoder;
import io.sedna.dna.DnaEncoder;
import java.util.Arrays;

/** Validates graphs survive canonical DNA encode/decode (codegen proxy for MVP). */
public final class DnaRoundTripCodegenProbe implements CodegenProbe {

  private final DnaEncoder encoder;
  private final DnaDecoder decoder;

  public DnaRoundTripCodegenProbe(DnaEncoder encoder, DnaDecoder decoder) {
    this.encoder = encoder;
    this.decoder = decoder;
  }

  @Override
  public Result<Boolean, SemanticError> probe(SemanticGraph graph) {
    var encoded = encoder.encode(graph);
    if (!encoded.isOk()) {
      return Result.err(encoded.error());
    }
    var decoded = decoder.decode(encoded.value());
    if (!decoded.isOk()) {
      return Result.err(decoded.error());
    }
    var reencoded = encoder.encode(decoded.value());
    if (!reencoded.isOk()) {
      return Result.err(reencoded.error());
    }
    if (!Arrays.equals(encoded.value(), reencoded.value())) {
      return Result.err(
          SemanticError.global(
              ErrorCode.VALIDATION_FAILED, "DNA round-trip bytes diverged in codegen probe"));
    }
    return Result.ok(Boolean.TRUE);
  }
}
