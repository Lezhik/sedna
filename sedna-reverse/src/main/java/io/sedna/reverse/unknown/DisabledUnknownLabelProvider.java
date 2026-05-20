package io.sedna.reverse.unknown;

import io.sedna.reverse.model.ParsedClass;

/** Deterministic heuristic labels when LLM enrichment is disabled. */
public final class DisabledUnknownLabelProvider implements UnknownNodeEnrichmentStep.UnknownLabelProvider {

  /** Singleton heuristic label provider. */
  public static final DisabledUnknownLabelProvider INSTANCE = new DisabledUnknownLabelProvider();

  private DisabledUnknownLabelProvider() {}

  @Override
  public String labelFor(ParsedClass parsed) {
    return parsed.simpleName();
  }
}
