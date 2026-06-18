package cc.attodao.mob_life.config;

import cc.attodao.mob_life.MobLife;
import cc.attodao.mob_life.morph.MorphType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

public final class MobLifeConfig {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path PATH =
      FabricLoader.getInstance().getConfigDir().resolve("mob_life.json");
  private static final Path LEGACY_PATH =
      FabricLoader.getInstance().getConfigDir().resolve("mob_life-client.json");
  private static final Config DEFAULT_CONFIG =
      new Config(
          new Gameplay(true),
          new Rendering(true),
          new Inventory(true, true, true),
          new Movement(true, true),
          new Debug(false));

  private static volatile Config config = DEFAULT_CONFIG;

  private MobLifeConfig() {}

  public static synchronized void load() {
    if (Files.exists(PATH)) {
      config = loadFrom(PATH);
      return;
    }

    if (Files.exists(LEGACY_PATH)) {
      config = loadFrom(LEGACY_PATH);
      save();
      return;
    }

    config = DEFAULT_CONFIG;
    save();
  }

  public static boolean playerMorphEnabled() {
    return config.gameplay().playerMorphEnabled();
  }

  public static boolean defaultPlayerMorphEnabled() {
    return DEFAULT_CONFIG.gameplay().playerMorphEnabled();
  }

  public static synchronized void setPlayerMorphEnabled(boolean playerMorphEnabled) {
    config = config.withGameplay(playerMorphEnabled);
  }

  public static boolean shaderEnabled() {
    return config.rendering().shaderEnabled();
  }

  public static boolean defaultShaderEnabled() {
    return DEFAULT_CONFIG.rendering().shaderEnabled();
  }

  public static synchronized void setShaderEnabled(boolean shaderEnabled) {
    config = config.withRendering(shaderEnabled);
  }

  public static boolean hotbarLimitEnabled() {
    return config.inventory().hotbarLimitEnabled();
  }

  public static boolean defaultHotbarLimitEnabled() {
    return DEFAULT_CONFIG.inventory().hotbarLimitEnabled();
  }

  public static synchronized void setHotbarLimitEnabled(boolean hotbarLimitEnabled) {
    config =
        config.withInventory(
            hotbarLimitEnabled,
            config.inventory().inventorySlotLimitEnabled(),
            config.inventory().offhandLimitEnabled());
  }

  public static boolean inventorySlotLimitEnabled() {
    return config.inventory().inventorySlotLimitEnabled();
  }

  public static boolean defaultInventorySlotLimitEnabled() {
    return DEFAULT_CONFIG.inventory().inventorySlotLimitEnabled();
  }

  public static synchronized void setInventorySlotLimitEnabled(boolean inventorySlotLimitEnabled) {
    config =
        config.withInventory(
            config.inventory().hotbarLimitEnabled(),
            inventorySlotLimitEnabled,
            config.inventory().offhandLimitEnabled());
  }

  public static boolean offhandLimitEnabled() {
    return config.inventory().offhandLimitEnabled();
  }

  public static boolean defaultOffhandLimitEnabled() {
    return DEFAULT_CONFIG.inventory().offhandLimitEnabled();
  }

  public static synchronized void setOffhandLimitEnabled(boolean offhandLimitEnabled) {
    config =
        config.withInventory(
            config.inventory().hotbarLimitEnabled(),
            config.inventory().inventorySlotLimitEnabled(),
            offhandLimitEnabled);
  }

  public static boolean miningSpeedChangeEnabled() {
    return config.movement().miningSpeedChangeEnabled();
  }

  public static boolean defaultMiningSpeedChangeEnabled() {
    return DEFAULT_CONFIG.movement().miningSpeedChangeEnabled();
  }

  public static synchronized void setMiningSpeedChangeEnabled(boolean miningSpeedChangeEnabled) {
    config = config.withMovement(miningSpeedChangeEnabled, config.movement().reachChangeEnabled());
  }

  public static boolean reachChangeEnabled() {
    return config.movement().reachChangeEnabled();
  }

  public static boolean defaultReachChangeEnabled() {
    return DEFAULT_CONFIG.movement().reachChangeEnabled();
  }

  public static synchronized void setReachChangeEnabled(boolean reachChangeEnabled) {
    config = config.withMovement(config.movement().miningSpeedChangeEnabled(), reachChangeEnabled);
  }

  public static boolean showAwkwardnessDebug() {
    return config.debug().showAwkwardnessDebug();
  }

  public static boolean defaultShowAwkwardnessDebug() {
    return DEFAULT_CONFIG.debug().showAwkwardnessDebug();
  }

  public static synchronized void setShowAwkwardnessDebug(boolean showAwkwardnessDebug) {
    config = config.withDebug(showAwkwardnessDebug);
  }

  public static boolean isMorphEnabled(MorphType morph) {
    return morph != null && (!morph.isPlayer() || playerMorphEnabled());
  }

  public static List<MorphType> selectableMorphs() {
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
    if (playerMorphEnabled()) {
      return MorphType.PLAYER;
    }

    for (MorphType morph : MorphType.values()) {
      if (!morph.isPlayer()) {
        return morph;
      }
    }
    return MorphType.PLAYER;
  }

  private static Config loadFrom(Path path) {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
      return parse(root);
    } catch (IOException | RuntimeException exception) {
      MobLife.LOGGER.warn("Failed to load config {}", path, exception);
      return DEFAULT_CONFIG;
    }
  }

  private static Config parse(JsonObject root) {
    Gameplay gameplay = parseGameplay(root);
    Rendering rendering = parseRendering(root);
    Inventory inventory = parseInventory(root);
    Movement movement = parseMovement(root);
    Debug debug = parseDebug(root);
    return new Config(gameplay, rendering, inventory, movement, debug);
  }

  private static Gameplay parseGameplay(JsonObject root) {
    JsonObject gameplay = object(root, "gameplay");
    boolean playerMorphEnabled =
        booleanValue(
            gameplay, "player_morph_enabled", DEFAULT_CONFIG.gameplay().playerMorphEnabled());
    return new Gameplay(playerMorphEnabled);
  }

  private static Rendering parseRendering(JsonObject root) {
    JsonObject rendering = object(root, "rendering");
    boolean shaderEnabled =
        booleanValue(rendering, "shader_enabled", DEFAULT_CONFIG.rendering().shaderEnabled());
    return new Rendering(shaderEnabled);
  }

  private static Inventory parseInventory(JsonObject root) {
    JsonObject inventory = object(root, "inventory");
    boolean hotbarLimitEnabled =
        booleanValue(
            inventory, "hotbar_limit_enabled", DEFAULT_CONFIG.inventory().hotbarLimitEnabled());
    boolean inventorySlotLimitEnabled =
        booleanValue(
            inventory,
            "inventory_slot_limit_enabled",
            DEFAULT_CONFIG.inventory().inventorySlotLimitEnabled());
    boolean offhandLimitEnabled =
        booleanValue(
            inventory, "offhand_limit_enabled", DEFAULT_CONFIG.inventory().offhandLimitEnabled());
    return new Inventory(hotbarLimitEnabled, inventorySlotLimitEnabled, offhandLimitEnabled);
  }

  private static Movement parseMovement(JsonObject root) {
    JsonObject movement = object(root, "movement");
    boolean miningSpeedChangeEnabled =
        booleanValue(
            movement,
            "mining_speed_change_enabled",
            DEFAULT_CONFIG.movement().miningSpeedChangeEnabled());
    boolean reachChangeEnabled =
        booleanValue(
            movement, "reach_change_enabled", DEFAULT_CONFIG.movement().reachChangeEnabled());
    return new Movement(miningSpeedChangeEnabled, reachChangeEnabled);
  }

  private static Debug parseDebug(JsonObject root) {
    JsonObject debug = object(root, "debug");
    boolean showAwkwardnessDebug = DEFAULT_CONFIG.debug().showAwkwardnessDebug();
    if (debug != null && debug.has("show_awkwardness_debug")) {
      showAwkwardnessDebug = debug.get("show_awkwardness_debug").getAsBoolean();
    } else if (root.has("showAwkwardnessDebug")) {
      showAwkwardnessDebug = root.get("showAwkwardnessDebug").getAsBoolean();
    }
    return new Debug(showAwkwardnessDebug);
  }

  private static JsonObject object(JsonObject root, String key) {
    if (!root.has(key) || !root.get(key).isJsonObject()) {
      return null;
    }
    return root.getAsJsonObject(key);
  }

  private static boolean booleanValue(JsonObject object, String key, boolean fallback) {
    if (object == null || !object.has(key)) {
      return fallback;
    }
    return object.get(key).getAsBoolean();
  }

  public static synchronized void save() {
    JsonObject root = new JsonObject();

    JsonObject gameplay = new JsonObject();
    gameplay.addProperty("player_morph_enabled", config.gameplay().playerMorphEnabled());
    root.add("gameplay", gameplay);

    JsonObject rendering = new JsonObject();
    rendering.addProperty("shader_enabled", config.rendering().shaderEnabled());
    root.add("rendering", rendering);

    JsonObject inventory = new JsonObject();
    inventory.addProperty("hotbar_limit_enabled", config.inventory().hotbarLimitEnabled());
    inventory.addProperty(
        "inventory_slot_limit_enabled", config.inventory().inventorySlotLimitEnabled());
    inventory.addProperty("offhand_limit_enabled", config.inventory().offhandLimitEnabled());
    root.add("inventory", inventory);

    JsonObject movement = new JsonObject();
    movement.addProperty(
        "mining_speed_change_enabled", config.movement().miningSpeedChangeEnabled());
    movement.addProperty("reach_change_enabled", config.movement().reachChangeEnabled());
    root.add("movement", movement);

    JsonObject debug = new JsonObject();
    debug.addProperty("show_awkwardness_debug", config.debug().showAwkwardnessDebug());
    root.add("debug", debug);

    try {
      Files.createDirectories(PATH.getParent());
      try (Writer writer = Files.newBufferedWriter(PATH, StandardCharsets.UTF_8)) {
        GSON.toJson(root, writer);
      }
    } catch (IOException exception) {
      MobLife.LOGGER.warn("Failed to save config {}", PATH, exception);
    }
  }

  private record Config(
      Gameplay gameplay, Rendering rendering, Inventory inventory, Movement movement, Debug debug) {

    Config withGameplay(boolean playerMorphEnabled) {
      return new Config(new Gameplay(playerMorphEnabled), rendering, inventory, movement, debug);
    }

    Config withRendering(boolean shaderEnabled) {
      return new Config(gameplay, new Rendering(shaderEnabled), inventory, movement, debug);
    }

    Config withInventory(
        boolean hotbarLimitEnabled,
        boolean inventorySlotLimitEnabled,
        boolean offhandLimitEnabled) {
      return new Config(
          gameplay,
          rendering,
          new Inventory(hotbarLimitEnabled, inventorySlotLimitEnabled, offhandLimitEnabled),
          movement,
          debug);
    }

    Config withMovement(boolean miningSpeedChangeEnabled, boolean reachChangeEnabled) {
      return new Config(
          gameplay,
          rendering,
          inventory,
          new Movement(miningSpeedChangeEnabled, reachChangeEnabled),
          debug);
    }

    Config withDebug(boolean showAwkwardnessDebug) {
      return new Config(gameplay, rendering, inventory, movement, new Debug(showAwkwardnessDebug));
    }
  }

  private record Gameplay(boolean playerMorphEnabled) {}

  private record Rendering(boolean shaderEnabled) {}

  private record Inventory(
      boolean hotbarLimitEnabled, boolean inventorySlotLimitEnabled, boolean offhandLimitEnabled) {}

  private record Movement(boolean miningSpeedChangeEnabled, boolean reachChangeEnabled) {}

  private record Debug(boolean showAwkwardnessDebug) {}
}
