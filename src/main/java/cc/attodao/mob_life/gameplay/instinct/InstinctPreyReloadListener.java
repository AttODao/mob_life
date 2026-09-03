package cc.attodao.mob_life.gameplay.instinct;

import cc.attodao.mob_life.MobLife;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;

public final class InstinctPreyReloadListener
    extends SimplePreparableReloadListener<Map<EntityType<?>, InstinctPreyManager.FoodValue>> {
  private static final Identifier RESOURCE = MobLife.id("mob_life/instinct/prey.json");

  @Override
  protected Map<EntityType<?>, InstinctPreyManager.FoodValue> prepare(
      ResourceManager resourceManager, ProfilerFiller profiler) {
    Map<EntityType<?>, InstinctPreyManager.FoodValue> loaded = new HashMap<>();
    List<Resource> stack = resourceManager.getResourceStack(RESOURCE);
    for (Resource resource : stack) {
      try (Reader reader = resource.openAsReader()) {
        JsonObject root = StrictJsonParser.parse(reader).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
          applyEntry(loaded, entry.getKey(), entry.getValue(), resource.sourcePackId());
        }
      } catch (Exception exception) {
        MobLife.LOGGER.error(
            "Ignoring invalid instinct prey resource from {}", resource.sourcePackId(), exception);
      }
    }
    return Map.copyOf(loaded);
  }

  private static void applyEntry(
      Map<EntityType<?>, InstinctPreyManager.FoodValue> loaded,
      String rawId,
      JsonElement element,
      String packId) {
    Identifier id = Identifier.tryParse(rawId);
    EntityType<?> type = id != null ? BuiltInRegistries.ENTITY_TYPE.getValue(id) : null;
    if (id == null || type == null) {
      MobLife.LOGGER.error("Disabling unknown instinct prey {} from {}", rawId, packId);
      return;
    }

    try {
      JsonObject value = element.getAsJsonObject();
      if (value.has("enabled") && !value.get("enabled").getAsBoolean()) {
        loaded.remove(type);
        return;
      }
      double rawNutrition = value.get("nutrition").getAsDouble();
      if (!Double.isFinite(rawNutrition)
          || rawNutrition < 0.0
          || rawNutrition > Integer.MAX_VALUE
          || rawNutrition != Math.rint(rawNutrition)) {
        throw new IllegalArgumentException("nutrition must be a non-negative integer");
      }
      float saturation = value.get("saturation_modifier").getAsFloat();
      loaded.put(type, new InstinctPreyManager.FoodValue((int) rawNutrition, saturation));
    } catch (Exception exception) {
      loaded.remove(type);
      MobLife.LOGGER.error("Disabling invalid instinct prey {} from {}", rawId, packId, exception);
    }
  }

  @Override
  protected void apply(
      Map<EntityType<?>, InstinctPreyManager.FoodValue> loaded,
      ResourceManager resourceManager,
      ProfilerFiller profiler) {
    InstinctPreyManager.replace(loaded);
    MobLife.LOGGER.info("Loaded {} instinct prey definitions", loaded.size());
  }
}
