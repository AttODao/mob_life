package cc.attodao.mob_life.config;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;

public final class MorphConfigReloadListener
    extends SimplePreparableReloadListener<Map<MorphType, MorphConfig>> {
  private static final FileToIdConverter CONVERTER = FileToIdConverter.json("mob_life/morphs");

  @Override
  protected Map<MorphType, MorphConfig> prepare(
      ResourceManager resourceManager, ProfilerFiller profiler) {
    EnumMap<MorphType, MorphConfig> loaded = new EnumMap<>(MorphType.class);
    boolean invalid = false;
    for (Map.Entry<Identifier, List<Resource>> entry :
        CONVERTER.listMatchingResourceStacks(resourceManager).entrySet()) {
      Identifier configId = CONVERTER.fileToId(entry.getKey());
      MorphType morph = MorphType.fromId(configId.getPath());
      if (!configId.getNamespace().equals(MobLife.MOD_ID)
          || morph.isPlayer()
          || !morph.id().equals(configId.getPath())) {
        for (Resource resource : entry.getValue()) {
          MobLife.LOGGER.error(
              "Invalid morph config path {} from {}: unknown namespace/form or unsupported player.json",
              entry.getKey(),
              resource.sourcePackId());
        }
        invalid = true;
        continue;
      }

      JsonObject merged = new JsonObject();
      MorphConfig config = null;
      for (Resource resource : entry.getValue()) {
        try (Reader reader = resource.openAsReader()) {
          JsonObject layer = StrictJsonParser.parse(reader).getAsJsonObject();
          MorphConfigCodec.validateLayer(morph, layer);
          JsonObject candidate = merge(merged, layer);
          MorphConfig parsed = MorphConfig.fromJson(morph, candidate);
          merged = candidate;
          config = parsed;
        } catch (Exception exception) {
          MobLife.LOGGER.error(
              "Invalid morph config path {} from {}: {}",
              entry.getKey(),
              resource.sourcePackId(),
              reason(exception));
          invalid = true;
        }
      }
      if (config != null) {
        loaded.put(morph, config);
      }
    }
    return invalid ? Map.of() : loaded;
  }

  private static String reason(Exception exception) {
    String message = exception.getMessage();
    return message != null && !message.isBlank() ? message : exception.getClass().getSimpleName();
  }

  private static JsonObject merge(JsonObject base, JsonObject override) {
    JsonObject merged = base.deepCopy();
    for (Map.Entry<String, JsonElement> entry : override.entrySet()) {
      String key = entry.getKey();
      JsonElement value = entry.getValue();
      JsonElement existing = merged.get(key);
      if (existing != null && existing.isJsonObject() && value.isJsonObject()) {
        merged.add(key, merge(existing.getAsJsonObject(), value.getAsJsonObject()));
      } else {
        merged.add(key, value.deepCopy());
      }
    }
    return merged;
  }

  @Override
  protected void apply(
      Map<MorphType, MorphConfig> loaded,
      ResourceManager resourceManager,
      ProfilerFiller profiler) {
    MorphConfigManager.replace(loaded);
    if (loaded.isEmpty()) {
      MobLife.LOGGER.error("Morph config reload failed; installed all built-in configs");
    } else {
      MobLife.LOGGER.info("Loaded {} Mob Life morph configs", loaded.size());
    }
  }
}
