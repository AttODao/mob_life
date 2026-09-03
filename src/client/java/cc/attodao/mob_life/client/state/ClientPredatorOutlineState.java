package cc.attodao.mob_life.client.state;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientPredatorOutlineState {
  private static Set<Integer> predators = Set.of();
  private static Set<Integer> prey = Set.of();

  private ClientPredatorOutlineState() {}

  public static void set(List<Integer> predatorIds, List<Integer> preyIds) {
    predators = Set.copyOf(new HashSet<>(predatorIds));
    prey = Set.copyOf(new HashSet<>(preyIds));
  }

  public static boolean contains(int entityId) {
    return predators.contains(entityId);
  }

  public static boolean containsPrey(int entityId) {
    return prey.contains(entityId);
  }

  public static void clear() {
    predators = Set.of();
    prey = Set.of();
  }
}
