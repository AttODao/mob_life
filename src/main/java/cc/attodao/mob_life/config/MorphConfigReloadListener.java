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
    for (Map.Entry<Identifier, List<Resource>> entry :
        CONVERTER.listMatchingResourceStacks(resourceManager).entrySet()) {
      Identifier configId = CONVERTER.fileToId(entry.getKey());
      MorphType morph = MorphType.fromId(configId.getPath());
      if (!morph.id().equals(configId.getPath())) {
        MobLife.LOGGER.warn("Ignoring unknown morph config {}", configId);
        continue;
      }

      JsonObject merged = new JsonObject();
      MorphConfig config = null;
      for (Resource resource : entry.getValue()) {
        try (Reader reader = resource.openAsReader()) {
          JsonObject candidate = merge(merged, StrictJsonParser.parse(reader).getAsJsonObject());
          MorphConfig parsed = MorphConfig.fromJson(morph, candidate);
          merged = candidate;
          config = parsed;
        } catch (Exception exception) {
          MobLife.LOGGER.error(
              "Ignoring invalid morph config {} from {}; using the previous valid resource",
              configId,
              resource.sourcePackId(),
              exception);
        }
      }
      if (config != null) {
        loaded.put(morph, config);
      }
    }
    return loaded;
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
    MobLife.LOGGER.info("Loaded {} Mob Life morph configs", loaded.size());
  }
}
