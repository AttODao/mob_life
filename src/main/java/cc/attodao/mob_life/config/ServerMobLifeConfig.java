package cc.attodao.mob_life.config;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/** Server-authoritative settings. A connected client only reads the synchronized copy. */
public final class ServerMobLifeConfig {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path PATH =
      FabricLoader.getInstance().getConfigDir().resolve("mob_life-server.json");
  private static final Path LEGACY_PATH =
      FabricLoader.getInstance().getConfigDir().resolve("mob_life.json");
  private static final Settings DEFAULT_SETTINGS =
      new Settings(true, true, true, true, true, true, true);

  private static volatile Settings localSettings = DEFAULT_SETTINGS;
  private static volatile Settings synchronizedSettings;

  private ServerMobLifeConfig() {}

  public static synchronized void load() {
    if (Files.exists(PATH)) {
      localSettings = loadFrom(PATH);
      return;
    }

    localSettings = Files.exists(LEGACY_PATH) ? loadFrom(LEGACY_PATH) : DEFAULT_SETTINGS;
    save();
  }

  public static boolean morphEnabled() {
    return effective().morphEnabled();
  }

  public static boolean playerMorphEnabled() {
    return effective().playerMorphEnabled();
  }

  public static boolean hotbarLimitEnabled() {
    return effective().hotbarLimitEnabled();
  }

  public static boolean inventorySlotLimitEnabled() {
    return effective().inventorySlotLimitEnabled();
  }

  public static boolean offhandLimitEnabled() {
    return effective().offhandLimitEnabled();
  }

  public static boolean miningSpeedChangeEnabled() {
    return effective().miningSpeedChangeEnabled();
  }

  public static boolean reachChangeEnabled() {
    return effective().reachChangeEnabled();
  }

  public static boolean isMorphEnabled(MorphType morph) {
    return morph != null
        && (morph.isPlayer() ? !morphEnabled() || playerMorphEnabled() : morphEnabled());
  }

  public static List<MorphType> selectableMorphs() {
    if (!morphEnabled()) {
      return List.of(MorphType.PLAYER);
    }

    ArrayList<MorphType> morphs = new ArrayList<>(MorphType.values().length);
    if (playerMorphEnabled()) {
      morphs.add(MorphType.PLAYER);
    }
    for (MorphType morph : MorphType.values()) {
      if (!morph.isPlayer()) {
        morphs.add(morph);
      }
    }
    return List.copyOf(morphs);
  }

  public static MorphType defaultMorph() {
    if (!morphEnabled() || playerMorphEnabled()) {
      return MorphType.PLAYER;
    }
    for (MorphType morph : MorphType.values()) {
      if (!morph.isPlayer()) {
        return morph;
      }
    }
    return MorphType.PLAYER;
  }

  public static Settings settings() {
    return effective();
  }

  /**
   * Installs values received from the connected server; no clientbound setting is writable here.
   */
  public static void installSynchronized(Settings settings) {
    synchronizedSettings = settings != null ? settings : null;
  }

  public static void clearSynchronized() {
    synchronizedSettings = null;
  }

  public static synchronized void save() {
    JsonObject root = new JsonObject();
    JsonObject gameplay = new JsonObject();
    gameplay.addProperty("morph_enabled", localSettings.morphEnabled());
    gameplay.addProperty("player_morph_enabled", localSettings.playerMorphEnabled());
    root.add("gameplay", gameplay);

    JsonObject inventory = new JsonObject();
    inventory.addProperty("hotbar_limit_enabled", localSettings.hotbarLimitEnabled());
    inventory.addProperty(
        "inventory_slot_limit_enabled", localSettings.inventorySlotLimitEnabled());
    inventory.addProperty("offhand_limit_enabled", localSettings.offhandLimitEnabled());
    root.add("inventory", inventory);

    JsonObject movement = new JsonObject();
    movement.addProperty("mining_speed_change_enabled", localSettings.miningSpeedChangeEnabled());
    movement.addProperty("reach_change_enabled", localSettings.reachChangeEnabled());
    root.add("movement", movement);

    try {
      Files.createDirectories(PATH.getParent());
      try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
        GSON.toJson(root, writer);
      }
    } catch (IOException exception) {
      MobLife.LOGGER.warn("Failed to save server config {}", PATH, exception);
    }
  }

  private static Settings effective() {
    Settings synchronizedCopy = synchronizedSettings;
    return synchronizedCopy != null ? synchronizedCopy : localSettings;
  }

  private static Settings loadFrom(Path path) {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
      JsonObject gameplay = object(root, "gameplay");
      JsonObject inventory = object(root, "inventory");
      JsonObject movement = object(root, "movement");
      return new Settings(
          bool(gameplay, "morph_enabled", DEFAULT_SETTINGS.morphEnabled()),
          bool(gameplay, "player_morph_enabled", DEFAULT_SETTINGS.playerMorphEnabled()),
          bool(inventory, "hotbar_limit_enabled", DEFAULT_SETTINGS.hotbarLimitEnabled()),
          bool(
              inventory,
              "inventory_slot_limit_enabled",
              DEFAULT_SETTINGS.inventorySlotLimitEnabled()),
          bool(inventory, "offhand_limit_enabled", DEFAULT_SETTINGS.offhandLimitEnabled()),
          bool(
              movement, "mining_speed_change_enabled", DEFAULT_SETTINGS.miningSpeedChangeEnabled()),
          bool(movement, "reach_change_enabled", DEFAULT_SETTINGS.reachChangeEnabled()));
    } catch (IOException | RuntimeException exception) {
      MobLife.LOGGER.warn("Failed to load server config {}", path, exception);
      return DEFAULT_SETTINGS;
    }
  }

  private static JsonObject object(JsonObject root, String key) {
    JsonElement element = root.get(key);
    return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
  }

  private static boolean bool(JsonObject object, String key, boolean fallback) {
    JsonElement element = object.get(key);
    try {
      return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
    } catch (RuntimeException exception) {
      return fallback;
    }
  }

  public record Settings(
      boolean morphEnabled,
      boolean playerMorphEnabled,
      boolean hotbarLimitEnabled,
      boolean inventorySlotLimitEnabled,
      boolean offhandLimitEnabled,
      boolean miningSpeedChangeEnabled,
      boolean reachChangeEnabled) {}
}
