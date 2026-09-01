package cc.attodao.mob_life.client.state;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientPredatorOutlineState {
  private static Set<Integer> predators = Set.of();

  private ClientPredatorOutlineState() {}

  public static void set(List<Integer> predatorIds) {
    predators = Set.copyOf(new HashSet<>(predatorIds));
  }

  public static boolean contains(int entityId) {
    return predators.contains(entityId);
  }

  public static void clear() {
    predators = Set.of();
  }
}
