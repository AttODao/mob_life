package cc.attodao.mob_life.config;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.EnumMap;
import java.util.Map;

public final class MorphConfigManager {
  private static volatile Map<MorphType, MorphConfig> configs = defaults();

  private MorphConfigManager() {}

  public static MorphConfig get(MorphType morph) {
    MorphType resolved = morph != null ? morph : MorphType.PLAYER;
    return configs.getOrDefault(resolved, MorphConfig.defaults(resolved));
  }

  static void replace(Map<MorphType, MorphConfig> loaded) {
    EnumMap<MorphType, MorphConfig> merged = new EnumMap<>(MorphType.class);
    for (MorphType morph : MorphType.values()) {
      merged.put(morph, loaded.getOrDefault(morph, MorphConfig.defaults(morph)));
    }
    configs = Map.copyOf(merged);
  }

  public static String encode(MorphType morph) {
    return get(morph).toJson().toString();
  }

  public static void installSynced(MorphType morph, String json) {
    try {
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      EnumMap<MorphType, MorphConfig> updated = new EnumMap<>(configs);
      updated.put(morph, MorphConfig.fromJson(morph, root));
      configs = Map.copyOf(updated);
    } catch (RuntimeException exception) {
      MobLife.LOGGER.error("Could not decode synced morph config for {}", morph.id(), exception);
    }
  }

  private static Map<MorphType, MorphConfig> defaults() {
    EnumMap<MorphType, MorphConfig> defaults = new EnumMap<>(MorphType.class);
    for (MorphType morph : MorphType.values()) {
      defaults.put(morph, MorphConfig.defaults(morph));
    }
    return Map.copyOf(defaults);
  }
}
