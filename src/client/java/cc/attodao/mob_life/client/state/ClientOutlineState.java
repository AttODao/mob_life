package cc.attodao.mob_life.client.state;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientOutlineState {
  private static Set<Integer> predators = Set.of();
  private static Set<Integer> prey = Set.of();

  private ClientOutlineState() {}

  public static void set(List<Integer> predatorIds, List<Integer> preyIds) {
    predators = Set.copyOf(new HashSet<>(predatorIds));
    HashSet<Integer> preyOnly = new HashSet<>(preyIds);
    preyOnly.removeAll(predators);
    prey = Set.copyOf(preyOnly);
  }

  public static int color(int entityId) {
    if (predators.contains(entityId)) {
      return 0xFFFF3B30;
    }
    return prey.contains(entityId) ? 0xFF45D483 : 0;
  }

  public static void clear() {
    predators = Set.of();
    prey = Set.of();
  }
}
