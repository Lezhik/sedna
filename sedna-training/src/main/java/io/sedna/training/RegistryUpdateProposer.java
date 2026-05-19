package io.sedna.training;

import io.sedna.core.CanonicalOrdering;
import io.sedna.core.GenomeNode;
import io.sedna.core.SemanticCore;
import io.sedna.core.SemanticGraph;
import io.sedna.core.VocabRef;
import io.sedna.registry.SemanticRegistry;
import io.sedna.training.model.RegistryUpdateProposal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Proposes registry updates with deterministic conflict resolution. */
public final class RegistryUpdateProposer {

  public static final String SKIP_EXACT = "SKIP_EXACT_MATCH";
  public static final String APPEND_VERSION = "APPEND_NEW_VERSION";
  public static final String MANUAL_REVIEW = "MANUAL_REVIEW";

  private final SemanticRegistry registry;

  public RegistryUpdateProposer(SemanticRegistry registry) {
    this.registry = registry;
  }

  public List<RegistryUpdateProposal> propose(SemanticGraph graph) {
    SemanticGraph canonical = CanonicalOrdering.canonicalize(graph);
    Set<String> seen = new TreeSet<>();
    List<RegistryUpdateProposal> proposals = new ArrayList<>();

    for (GenomeNode node : canonical.nodes()) {
      for (VocabRef ref : vocabularyRefs(node.core())) {
        String key = ref.canonicalKey();
        if (!seen.add(key)) {
          continue;
        }
        proposals.add(resolveProposal(ref));
      }
    }

    proposals.sort(
        Comparator.comparing((RegistryUpdateProposal p) -> p.proposed().canonicalKey())
            .thenComparing(RegistryUpdateProposal::resolution));
    return List.copyOf(proposals);
  }

  private RegistryUpdateProposal resolveProposal(VocabRef ref) {
    if (ref.termPath().contains("UNKNOWN")) {
      return new RegistryUpdateProposal(ref, MANUAL_REVIEW, Optional.empty());
    }
    var resolved = registry.resolve(ref);
    if (resolved.isOk()) {
      return new RegistryUpdateProposal(ref, SKIP_EXACT, Optional.empty());
    }
    String message = resolved.error().message().toLowerCase(Locale.ROOT);
    if (message.contains("version")) {
      return new RegistryUpdateProposal(ref, APPEND_VERSION, Optional.empty());
    }
    return new RegistryUpdateProposal(ref, MANUAL_REVIEW, Optional.empty());
  }

  private static List<VocabRef> vocabularyRefs(SemanticCore core) {
    Set<VocabRef> refs = new LinkedHashSet<>();
    refs.add(core.classRef());
    refs.add(core.targetRef());
    refs.add(core.operationRef());
    return List.copyOf(refs);
  }
}
