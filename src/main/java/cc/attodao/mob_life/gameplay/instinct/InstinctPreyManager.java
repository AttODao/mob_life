package cc.attodao.mob_life.gameplay.instinct;

import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.EntityType;

public final class InstinctPreyManager {
  private static volatile Map<EntityType<?>, FoodValue> values = Map.of();

  private InstinctPreyManager() {}

  public static Optional<FoodValue> get(EntityType<?> type) {
    return Optional.ofNullable(values.get(type));
  }

  public static boolean isEnabled(EntityType<?> type) {
    return values.containsKey(type);
  }

  static void replace(Map<EntityType<?>, FoodValue> loaded) {
    values = Map.copyOf(loaded);
  }

  public record FoodValue(int nutrition, float saturationModifier) {
    public FoodValue {
      if (nutrition < 0) {
        throw new IllegalArgumentException("Prey nutrition must be non-negative");
      }
      if (!Float.isFinite(saturationModifier) || saturationModifier < 0.0F) {
        throw new IllegalArgumentException(
            "Prey saturation_modifier must be finite and non-negative");
      }
    }
  }
}
