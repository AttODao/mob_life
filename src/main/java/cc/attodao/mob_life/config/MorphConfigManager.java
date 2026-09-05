package cc.attodao.mob_life.config;

import cc.attodao.mob_life.morph.MorphType;
import cc.attodao.mob_life.network.MobLifeNetworking.MorphConfigEntry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MorphConfigManager {
  private static volatile Map<MorphType, MorphConfig> configs = defaults();
  private static volatile long generation;

  private MorphConfigManager() {}

  public static MorphConfig get(MorphType morph) {
    if (morph == null || morph.isPlayer()) {
      throw new IllegalArgumentException("The player form does not have a morph config");
    }
    return configs.getOrDefault(morph, MorphConfig.defaults(morph));
  }

  static void replace(Map<MorphType, MorphConfig> loaded) {
    EnumMap<MorphType, MorphConfig> merged = new EnumMap<>(MorphType.class);
    for (MorphType morph : MorphType.values()) {
      if (!morph.isPlayer()) {
        merged.put(morph, loaded.getOrDefault(morph, MorphConfig.defaults(morph)));
      }
    }
    configs = Map.copyOf(merged);
    generation++;
  }

  public static long generation() {
    return generation;
  }

  public static void markRuntimeProfilesChanged() {
    generation++;
  }

  public static Map<MorphType, MorphConfig> snapshot() {
    return configs;
  }

  public static String encode(MorphType morph) {
    return get(morph).toJson().toString();
  }

  public static void installSyncedBatch(long syncedGeneration, List<MorphConfigEntry> entries) {
    EnumMap<MorphType, MorphConfig> decoded = new EnumMap<>(MorphType.class);
    Set<String> seen = new HashSet<>();
    for (MorphConfigEntry entry : entries) {
      if (!seen.add(entry.morphId())) {
        throw new IllegalArgumentException("Duplicate synced morph " + entry.morphId());
      }
      MorphType morph = MorphType.fromId(entry.morphId());
      if (morph.isPlayer() || !morph.id().equals(entry.morphId())) {
        throw new IllegalArgumentException("Unknown synced morph " + entry.morphId());
      }
      JsonObject root = JsonParser.parseString(entry.configJson()).getAsJsonObject();
      MorphConfigCodec.validateLayer(morph, root);
      decoded.put(morph, MorphConfig.fromJson(morph, root));
    }
    for (MorphType morph : MorphType.values()) {
      if (!morph.isPlayer() && !decoded.containsKey(morph)) {
        throw new IllegalArgumentException("Missing synced morph " + morph.id());
      }
    }
    configs = Map.copyOf(decoded);
    generation = syncedGeneration;
  }

  private static Map<MorphType, MorphConfig> defaults() {
    EnumMap<MorphType, MorphConfig> defaults = new EnumMap<>(MorphType.class);
    for (MorphType morph : MorphType.values()) {
      if (!morph.isPlayer()) {
        defaults.put(morph, MorphConfig.defaults(morph));
      }
    }
    return Map.copyOf(defaults);
  }
}
