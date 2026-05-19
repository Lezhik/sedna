package io.sedna.reverse.structural;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tarjan SCC for cycle detection on structural graphs. */
public final class TarjanScc {

  private TarjanScc() {}

  public static List<List<String>> stronglyConnectedComponents(
      List<String> nodes, Map<String, List<String>> adjacency) {
    Map<String, Integer> index = new HashMap<>();
    Map<String, Integer> lowLink = new HashMap<>();
    Map<String, Boolean> onStack = new HashMap<>();
    Deque<String> stack = new ArrayDeque<>();
    List<List<String>> components = new ArrayList<>();
    int[] counter = {0};

    for (String node : nodes) {
      if (!index.containsKey(node)) {
        strongConnect(node, adjacency, index, lowLink, onStack, stack, components, counter);
      }
    }
    return components;
  }

  private static void strongConnect(
      String node,
      Map<String, List<String>> adjacency,
      Map<String, Integer> index,
      Map<String, Integer> lowLink,
      Map<String, Boolean> onStack,
      Deque<String> stack,
      List<List<String>> components,
      int[] counter) {
    index.put(node, counter[0]);
    lowLink.put(node, counter[0]);
    counter[0]++;
    stack.push(node);
    onStack.put(node, true);

    for (String successor : adjacency.getOrDefault(node, List.of())) {
      if (!index.containsKey(successor)) {
        strongConnect(successor, adjacency, index, lowLink, onStack, stack, components, counter);
        lowLink.put(node, Math.min(lowLink.get(node), lowLink.get(successor)));
      } else if (onStack.getOrDefault(successor, false)) {
        lowLink.put(node, Math.min(lowLink.get(node), index.get(successor)));
      }
    }

    if (lowLink.get(node).equals(index.get(node))) {
      List<String> component = new ArrayList<>();
      String current;
      do {
        current = stack.pop();
        onStack.put(current, false);
        component.add(current);
      } while (!current.equals(node));
      component.sort(String::compareTo);
      components.add(List.copyOf(component));
    }
  }
}
